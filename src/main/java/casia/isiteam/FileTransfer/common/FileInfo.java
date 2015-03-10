package casia.isiteam.FileTransfer.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class FileInfo {
	String fileName;
	long fileLength;
	
	public FileInfo(File file) throws FileNotFoundException{
		this.fileName=file.getName();
		this.fileLength=file.length();
	}
	public FileInfo(String filePath,long fileLength) throws FileNotFoundException{
		File file=new File(filePath);
		this.fileName=file.getName();
		this.fileLength=file.length();
	}
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public long getFileLength() {
		return fileLength;
	}
	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}

}
