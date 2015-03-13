package casia.isiteam.videosearch.util.filetransfer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import casia.isiteam.videosearch.util.Util;
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

public class FileReceiver implements Callable<Void> {
	String host;
	int fileTransferPort;
	String fileDir=null;
	boolean useFileNameOnClient=false;

	ExecutorService selfExecutor=Executors.newSingleThreadExecutor();
	
	AtomicInteger fileNum=new AtomicInteger(2);
	
	public FileReceiver(String host, int fileTransferPort, String fileDir ,boolean useFileNameOnClient){
		this.host=host;
		this.fileTransferPort=fileTransferPort;
		this.fileDir=fileDir;
		this.useFileNameOnClient=useFileNameOnClient;
	}
	@Override
	public Void call() throws Exception{
		
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
							cp.addLast(new LoggingHandler(LogLevel.INFO));
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
			Util.printContextInfo(null);
			throw e;
		}
		finally{
			group.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
		return null;		
	}
	public void start(){
		selfExecutor.submit(this);
	}
	public String getNextFileName(String fileNameOnClient) {

		if(this.useFileNameOnClient){
			return fileNameOnClient;
		}
		
		int ind = fileNameOnClient.lastIndexOf('.');
		String extString="";
		if(ind !=-1){
			extString = fileNameOnClient.substring(ind,
					fileNameOnClient.length());
		}
		return fileNum.getAndIncrement() + extString;
	}
	
	public String getFileDir() {
		return fileDir;
	}
	
	public static void main(String [] args){
		
		FileReceiver fileReceiver=new FileReceiver("0.0.0.0",9001,"C:/",false);		
		fileReceiver.start();
	}
}
