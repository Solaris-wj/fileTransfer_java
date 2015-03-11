package casia.isiteam.testNetty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class App2 {

	public static void main(String[] args){
		ByteBuf buf=Unpooled.buffer();
		System.out.println(buf.capacity());
		int num=1000;
		
		for(int i=0; i<num;++i){
			
			buf.writeInt(i);
		}
		
		System.out.println(buf.capacity());
	}
}
