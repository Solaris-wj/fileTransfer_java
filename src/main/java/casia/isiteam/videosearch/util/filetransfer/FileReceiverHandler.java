package casia.isiteam.videosearch.util.filetransfer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import casia.isiteam.videosearch.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class FileReceiverHandler extends ChannelHandlerAdapter {

	File file = null;
	long fileLength=0;
	FileReceiver fileReceiver = null;
	OutputStream out = null;
	BufferedOutputStream bout = null;
	long readedSize = 0;

	public FileReceiverHandler(FileReceiver fileReceiver) {
		this.fileReceiver = fileReceiver;

	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {

		ByteBuf buf = (ByteBuf) msg;

		
		if (file == null) {
			int fileNameLength = buf.readInt();

			byte[] dst = new byte[fileNameLength];

			buf.readBytes(dst);
			String nameOnClient = new String(dst);
			fileLength = buf.readLong();			

			String nameOnServer = fileReceiver.getNextFileName(nameOnClient);
			file=new File(fileReceiver.getFileDir(), nameOnServer);
			out = new FileOutputStream(file);
			bout = new BufferedOutputStream(out);

			buf.resetReaderIndex();
			byte[] b = new byte[buf.readableBytes()];
			buf.readBytes(b);
		}

		readedSize += buf.readableBytes();

		byte[] b = new byte[buf.readableBytes()];
		buf.readBytes(b);
		bout.write(b);

		if (readedSize >= fileLength) {
			readedSize = 0;
			fileLength=0;
			bout.close();
			out.close();
			ctx.writeAndFlush(Unpooled.copiedBuffer(file.getName().getBytes()));
			file = null;		
			
//			System.out.println("received one file");
//			//这里接收完一个文件就关闭，但是发送两个文件是没问题的。！！
//			ctx.close().addListener(new ChannelFutureListener() {
//
//				@Override
//				public void operationComplete(ChannelFuture future)
//						throws Exception {
//					// TODO Auto-generated method stub
//					System.out.println("close");
//				}
//
//			});
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {

		// if (ctx.channel().isActive()) {
		// ctx.writeAndFlush(Unpooled.copiedBuffer(ResultCode.error.getBytes()));
		// }
		ctx.close();
		e.printStackTrace();

		Util.printContextInfo(null);
	}
}
