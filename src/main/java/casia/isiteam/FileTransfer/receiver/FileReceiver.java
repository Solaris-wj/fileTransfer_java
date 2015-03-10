package casia.isiteam.FileTransfer.receiver;

import java.io.File;

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
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class FileReceiver extends Thread{
	String host;
	int fileTransferPort;
	
	String fileDir="C:/";
	
	public FileReceiver(String host, int fileTransferPort){
		this.host=host;
		this.fileTransferPort=fileTransferPort;
		
	}
	@Override
	public void run(){
		EventLoopGroup group = new NioEventLoopGroup();
		EventLoopGroup workGroup = new NioEventLoopGroup();
		
		final FileReceiver fileReceiver=this;
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
							
							

							//encoder
							cp.addLast(new LengthFieldPrepender(Integer.BYTES));
							
							cp.addLast(new FileReceiverHandler(fileReceiver));
						}
					});

			ChannelFuture f;

			f = sBootstrap.bind(host, fileTransferPort).sync();

			//等待服务器关闭，会阻塞线程
			f.channel().closeFuture().sync();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//Util.printContextInfo(null);
		}
		finally{
			group.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
	}
	
	public String getNameFileName(String nameOnClient){
		
		return new File(fileDir,nameOnClient).getAbsolutePath();
	}
}
