package casia.isiteam.FileTransfer.common;

public class ResultCode {
	public static final String success="success";
	public static final String error="error";
	
	public String result=null;

	public boolean isSuccess(){
		if(result.equals(success)){
			return true;
		}
		else if(result.equals(error)){
			return false;
		}else {
			throw new NullPointerException("result code must be set before get it");
		}
	}
	public ResultCode setRetsult(String retsult) {
		this.result = retsult;
		return this;
	}
	
}
