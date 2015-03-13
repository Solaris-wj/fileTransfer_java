package casia.isiteam.videosearch.util.filetransfer;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import casia.isiteam.videosearch.util.Util;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public class FileSender extends FileSenderSuper implements Callable<Void> {

	long TimeOutPeriod = 30;
	String host;
	int fileTransferPort;
	ExecutorService selfExecutor = null;
	EventExecutor executor = null;
	AtomicBoolean isShutDown = null;
	AtomicBoolean started =new AtomicBoolean(false);

	ConcurrentLinkedQueue<Promise<String>> promiseQueue;
	Timer timer;

	public FileSender(String host, int fileTransferPort) {
		this.host = host;
		this.fileTransferPort = fileTransferPort;
	}

	/**
	 * 由于请求都是顺序发送，有一个没收到回复，后面的都认为是失败了
	 * 
	 * @author dell
	 *
	 */
	private class CheckPromise implements TimerTask {
		int lastPromiseSize = promiseQueue.size();

		@Override
		public void run(Timeout timeout) throws Exception {
			// 如果队列为空 什么也不做
			if (promiseQueue.size() == 0) {
				return;
			} // 队列不空，有请求正在等待回复
			else if (lastPromiseSize == promiseQueue.size()) {
				// 设置失败，并且关闭连接
				setAllPromiseFailed(new TimeoutException("operation time out!"));
				// FileSender.this.forceClose();
				connChannel.close();
				return;
			} else {
				lastPromiseSize = promiseQueue.size();
				timer.newTimeout(this, TimeOutPeriod, TimeUnit.SECONDS);
			}
		}
	}

	synchronized private void setAllPromiseFailed(Throwable cause) {
		Promise[] promises = new Promise[promiseQueue.size()];
		promises = promiseQueue.toArray(promises);

		promiseQueue.clear();
		for (Promise<String> promise : promises) {
			promise.setFailure(cause);
		}
	}

	/**
	 * 启动长连接发送文件
	 */
	public void start() {
		selfExecutor = Executors.newSingleThreadExecutor();
		executor = new DefaultEventExecutor();
		isShutDown = new AtomicBoolean(false);
		promiseQueue = new ConcurrentLinkedQueue<Promise<String>>();
		timer = new HashedWheelTimer();
		timer.newTimeout(new CheckPromise(), TimeOutPeriod, TimeUnit.SECONDS);

		selfExecutor.submit(this);
	}

	@Override
	public Promise<String> getRequestPromise() {
		return promiseQueue.poll();
	}

	/**
	 * 关闭连接和线程组，保证任务都完成，调用后由timer负责等待所有请求都接收到回复，然后关闭
	 */
	@Override
	public void releaseResources() {

		isShutDown.set(true);
		// 等待所有队列中的promise收到回复后，由handler调用forceclose关闭
		if (promiseQueue.size() == 0) {
			connChannel.close();
		} else {
			timer.newTimeout(new TimerTask() {

				@Override
				public void run(Timeout timeout) throws Exception {
					if (promiseQueue.size() == 0) {
						connChannel.close();
					}
				}
			}, 1, TimeUnit.SECONDS);
		}

		connChannel.closeFuture().addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future)
					throws Exception {
				executor.shutdownGracefully(0, 0, TimeUnit.SECONDS);
				timer.stop();
				selfExecutor.shutdown();
			}

		});

	}

	public void forceClose() {
		isShutDown.set(true);
		connChannel.close();
	}

	@Override
	public Void call() throws Exception {
		super.connect(host, fileTransferPort);
		
		return null;
	}

	private class sendFile implements Callable<String> {
		File file = null;

		public sendFile(File file) {
			this.file = file;
		}

		@Override
		public String call() throws Exception {
			byte[] b = file.getName().getBytes();
			ByteBuf buf = connChannel.alloc().buffer();
			buf.writeInt(b.length);
			buf.writeBytes(b);
			buf.writeLong(file.length());
			connChannel.writeAndFlush(buf);
			connChannel.writeAndFlush(new ChunkedFile(file));
			return null;
		}
	}

	@Override
	public String sendFile(File file) throws Exception {
		Future<String> ret = sendFileNoBlocking(file);
		return ret.get();
	}

	public Future<String> sendFileNoBlocking(File file) throws Exception {
		
		if (started.get() == false) {
			synchronized (this) {
				if (started.get() == false) {
					this.start();
				}
			}
		}
		
		// 如果关闭了就不能再发送
		if (isShutDown.get()) {
			throw new Exception("file send has been shut down");
		}
		executor.submit(new sendFile(file));

		// 写完之后 ，应该收到回复才确认真的完成了。
		Promise<String> ret = executor.newPromise();
		promiseQueue.offer(ret);
		return ret;
	}

	/**
	 * 短连接，直接建立连接发送文件，完成后关闭。阻塞等待！
	 * 
	 * @param file
	 * @param host
	 * @param fileTransferPort
	 * @return 成功0 失败-1
	 * @throws Exception
	 * @throws
	 */
	public static String sendFile(final File file, String host,
			int fileTransferPort) throws Exception {
		FileSender fileSender = null;
		try {
			fileSender = new FileSender(host, fileTransferPort);
			fileSender.start();
			synchronized (fileSender) {
				fileSender.wait();
			}
			Future<String> ret = fileSender.sendFileNoBlocking(file);

			return ret.get();
		} finally {
			fileSender.close();
		}
	}

	public static void main(String[] args) throws Exception {

		// String ret0 = FileSender.sendFile(new File("C:/t.txt"), "127.0.0.1",
		// 9001);
		// System.out.println(ret0);

		FileSender fileSender = new FileSender("127.0.0.1", 9001);

		fileSender.start();

		synchronized (fileSender) {
			fileSender.wait();
		}

		final FileSender finalFileSender = fileSender;
		final File file = new File("C:/t.txt");

		Future<String> ret = finalFileSender.sendFileNoBlocking(file);

		fileSender.forceClose();
		ret.await().sync();
		System.out.println("end of main");
	}

}
