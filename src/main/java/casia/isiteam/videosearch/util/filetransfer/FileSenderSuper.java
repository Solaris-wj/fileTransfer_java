package casia.isiteam.videosearch.util.filetransfer;

import io.netty.bootstrap.Bootstrap;
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
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public abstract class FileSenderSuper implements Closeable {

	EventLoopGroup connThreadGroup = null;
	Bootstrap bootstrap = null;
	Channel connChannel = null;

	protected void connect(String host, int port) {
		try {
			FileSenderSuper fileSender = this;
			connThreadGroup = new NioEventLoopGroup();
			bootstrap = new Bootstrap();
			bootstrap.channel(NioSocketChannel.class);
			bootstrap.group(connThreadGroup);
			bootstrap.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();

					// decoder
					p.addLast(new LoggingHandler(LogLevel.INFO));
					p.addLast(new LengthFieldBasedFrameDecoder(
							Integer.MAX_VALUE, 0, Integer.BYTES, 0,
							Integer.BYTES));
					p.addLast(new StringDecoder());

					// encoder
					p.addLast(new LengthFieldPrepender(Integer.BYTES));
					p.addLast(new ChunkedWriteHandler());

					// business logic
					p.addLast(new FileSenderHandler(fileSender));
				}
			});

			ChannelFuture f;

			f = bootstrap.connect(host, port).sync();

			synchronized (this) {
				this.notify();
			}

			this.connChannel = f.channel();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public abstract Promise<String> getRequestPromise();
	public abstract String sendFile(final File file) throws Exception ;
	
	public void releaseResources(){
		
	}
	@Override
	final public void close() throws IOException {
		connChannel.closeFuture().addListener(new ChannelFutureListener(){

			@Override
			public void operationComplete(ChannelFuture future)
					throws Exception {
				connThreadGroup.shutdownGracefully();				
			}			
		});
		
		releaseResources();
	}

}
