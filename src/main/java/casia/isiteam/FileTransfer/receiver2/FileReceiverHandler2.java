package casia.isiteam.FileTransfer.receiver2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;


public class FileReceiverHandler2 extends ChannelHandlerAdapter{
	
	FileReceiver2 fileReceiver;
	private String fileDir;
	private long fileLength=0;
	private File file;
	private FileOutputStream ofs;
	private long readedSize=0;
	
	AtomicInteger  cnt=new AtomicInteger(0);
	
	
	public FileReceiverHandler2(FileReceiver2 fileReceiver){
		this.fileReceiver=fileReceiver;
		this.fileDir=fileReceiver.fileDir;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		ByteBuf buf=(ByteBuf)msg;
		if(fileLength==0){
			
			//读取文件名长度
			int fileNameStrLength=buf.readInt();

			byte []dst=new byte[fileNameStrLength];
			buf.readBytes(dst);
			String fileNameOnClient=new String(dst);
			//读取文件大小
			fileLength=buf.readLong();
			
			String fileNameOnServer=fileReceiver.useFileNameOnClient?fileNameOnClient:fileReceiver.getNextFileName(fileNameOnClient);
			file=new File(fileDir,fileNameOnServer);
			ofs=new FileOutputStream(file);	
			
		}
		
		readedSize +=buf.readableBytes();
		if(buf.isReadable()){
			byte[] bytes=new byte[buf.readableBytes()];
			buf.readBytes(bytes);
			ofs.write(bytes);
		}	
		
		if(readedSize >= fileLength){
			readedSize=0;
			fileLength=0;
			file=null;
			ofs.flush();
			ofs.close();
			System.out.println("close file");
		}
		buf.release();
				
	}
}