package casia.isiteam.videosearch.util.filetransfer;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;

public class FileSenderHandler extends ChannelHandlerAdapter {

	FileSenderSuper fileSender;
	public FileSenderHandler(FileSenderSuper fileSender){
		this.fileSender=fileSender;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{			
		Promise<String> dp=fileSender.getRequestPromise();
		String ret=(String)msg;		
		dp.setSuccess(ret);		
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		
		cause.printStackTrace();
		throw new Exception(cause.getMessage());
	}
	
}
