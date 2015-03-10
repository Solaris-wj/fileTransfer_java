package casia.isiteam.FileTransfer.sender;

import java.util.concurrent.Callable;

import casia.isiteam.FileTransfer.common.FileInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SingleThreadEventExecutor;


public class FileSender extends Thread{
	String host;
	int fileTransferPort;
	Channel ch=null;
	EventExecutor executor=new DefaultEventExecutor();
	FileSenderHandler fileSenderHandler=new FileSenderHandler();
	public Future<Object> sendFile(FileInfo fileInfo) {
		
		
		Future<Void> f=ch.eventLoop().submit(new Callable<Void>() {

			public Void call() throws Exception {
				// TODO Auto-generated method stub
				
				ch.writeAndFlush("1");
				System.out.println("send success");
				return null;
			}
		});
		
		//写完之后 ，应该收到回复才确认真的完成了。
		Promise<Object> ret= executor.newPromise();
		
		fileSenderHandler.getPromiseQueue().offer(ret);
		return ret;	
	}
	@Override
	public void run(){
		
		EventLoopGroup group=new NioEventLoopGroup();
		
		//FileSender fileSender;
		try {
			Bootstrap bootstrap=new Bootstrap();
			bootstrap.channel(NioSocketChannel.class);
			bootstrap.group(group);
			bootstrap.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p=ch.pipeline();
					
					p.addLast(new LineBasedFrameDecoder(8192));
					p.addLast(new StringDecoder());
					
					p.addLast(new ChunkedWriteHandler());
					p.addLast(new StringEncoder());
					
				}
			});
			
			ChannelFuture f = bootstrap.connect(host, fileTransferPort).sync();			
			
			this.ch=f.channel();
			f.channel().closeFuture().sync();	
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			
		}
	}
}
