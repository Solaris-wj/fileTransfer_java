package casia.isiteam.FileTransfer.receiver2;

import java.util.concurrent.atomic.AtomicInteger;

import casia.isiteam.FileTransfer.receiver.FileReceiver;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class FileReceiver2 extends Thread {

	String host;
	int fileTransferPort;
	AtomicInteger fileNum = new AtomicInteger(2);
	String fileDir = "C:/";
	boolean useFileNameOnClient = false;
	
	public FileReceiver2(String host, int fileTransferPort){
		this.host=host;
		this.fileTransferPort=fileTransferPort;
		
	}
	
	public String getNextFileName(String fileNameOnClient) {

		int ind = fileNameOnClient.lastIndexOf('.');

		String extString = fileNameOnClient.substring(ind,
				fileNameOnClient.length());

		return fileNum.getAndIncrement() + extString;
	}

	public void connect(int port, String host) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		EventLoopGroup workGroup = new NioEventLoopGroup();
		
		final FileReceiver2 fileReceiver=this;
		try {
			ServerBootstrap sBootstrap = new ServerBootstrap();
			sBootstrap.group(group, workGroup);
			sBootstrap.channel(NioServerSocketChannel.class);
			sBootstrap.option(ChannelOption.TCP_NODELAY, true);

			sBootstrap
					.childHandler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel ch)
								throws Exception {

							ChannelPipeline cp = ch.pipeline();
							
							
							//decoder
							cp.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,Integer.BYTES,0,Integer.BYTES));
							cp.addLast(new LoggingHandler(LogLevel.INFO));
							//encoder
							cp.addLast(new LengthFieldPrepender(Integer.BYTES));
							
							cp.addLast(new FileReceiverHandler2(fileReceiver));
						}
					});

			ChannelFuture f;

			f = sBootstrap.bind(host, fileTransferPort).sync();

			//等待服务器关闭，会阻塞线程
			f.channel().closeFuture().sync();
		}finally{
			group.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {

		FileReceiver fileReceiver = new FileReceiver("0.0.0.0", 9001);

		fileReceiver.start();

	}

}
