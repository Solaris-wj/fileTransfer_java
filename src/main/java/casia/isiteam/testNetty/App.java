package casia.isiteam.testNetty;

import java.util.concurrent.Callable;

import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * Hello world!
 *
 */
public class App {
	
	public static void main(String[] args) {
		
		EventExecutor executor=new DefaultEventExecutor();
		
		Future<Void> f= executor.submit(new Callable<Void>() {

			public Void call() throws Exception {
				
				System.out.println(Thread.currentThread().getId());
				return null;
			}
			
		});
		
		f.addListener(new FutureListener<Void>() {

			public void operationComplete(
					io.netty.util.concurrent.Future<Void> future)
					throws Exception {
				System.out.println("complete");
				
			}
		
		});
		
		
		Promise<String> dp=executor.newPromise();
		
		dp.addListener(new FutureListener<String>() {	

			public void operationComplete(Future<String> future)
					throws Exception {
				
				System.out.println("new promise complete +" + Thread.currentThread().getId());
				
			}
			
		});
		dp.setSuccess(null);
		executor.shutdownGracefully();
	}
}