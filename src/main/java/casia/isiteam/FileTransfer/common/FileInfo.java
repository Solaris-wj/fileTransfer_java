package casia.isiteam.FileTransfer.common;

import java.io.File;
import java.io.FileNotFoundException;

public class FileInfo {
	String fileName;
	long fileLength;
	File file;
	
	public FileInfo(File file) throws FileNotFoundException{
		this.fileName=file.getName();
		this.fileLength=file.length();
		this.file=file;		
	}

	
	public FileInfo(String nameOnClient, long fileLength2) {
		// TODO Auto-generated constructor stub
		this.fileName=nameOnClient;
		this.fileLength=fileLength2;
		this.file=null;
	}


	public File getFile() {
		return file;
	}


	public void setFile(File file) {
		this.file = file;
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
