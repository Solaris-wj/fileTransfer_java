package casia.isiteam.FileTransfer.sender;

import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

public class FileSenderHandler extends ChannelHandlerAdapter {

	ConcurrentLinkedQueue<Promise<Object>> promiseQueue;
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
		Promise<Object> dp=promiseQueue.poll();
		dp.setSuccess(null);
	}
	public ConcurrentLinkedQueue<Promise<Object>> getPromiseQueue() {
		return promiseQueue;
	}
}
