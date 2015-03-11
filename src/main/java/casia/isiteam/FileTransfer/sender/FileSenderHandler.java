package casia.isiteam.FileTransfer.sender;

import casia.isiteam.FileTransfer.common.ResultCode;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;

public class FileSenderHandler extends ChannelHandlerAdapter {

	FileSender fileSender;
	public FileSenderHandler(FileSender fileSender){
		this.fileSender=fileSender;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception{
		System.out.println("channelActive");
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
		Promise<ResultCode> dp=fileSender.getPromiseQueue().poll();
		
		String ret=(String)msg;
		
		dp.setSuccess(new ResultCode().setRetsult(ret));
		
		
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		
		cause.printStackTrace();
	}
	
}
