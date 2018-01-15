package org.ak.trafficController.examples;

import org.ak.trafficController.TaskExecutor;

public class AsyncCallsExample {

	public static void main(String[] args) throws Throwable {
		//runWithTaskExecutor();
		runWithTaskExecutorOther();
	}

	protected static void runDirectly() {
		sync1();
		new Thread(()->{
			doAsync1();
		}).start();
		new Thread(()->{
			doAsync2();
		}).start();
		sync2();
		sync3();
		sync4();
		sync5();
	}

	protected static void runWithTaskExecutor() throws Throwable {
		TaskExecutor.getInstance()
		.of(AsyncCallsExample::sync1)
			.thenParallelWithoutWait(AsyncCallsExample::doAsync1, AsyncCallsExample::doAsync2)
		.getParentTask()
		.then(AsyncCallsExample::sync2)
		.then(AsyncCallsExample::sync3)
		.then(AsyncCallsExample::sync4)
		.then(AsyncCallsExample::sync5)
		.start();
	}
	
	
	protected static void runDirectlyOther() {
		sync1();
		new Thread(()->{
			doAsync1();
			doAsync2();
		}).start();
		sync2();
		sync3();
		sync4();
		sync5();
	}

	protected static void runWithTaskExecutorOther() throws Throwable {
		TaskExecutor.getInstance()
		.of(AsyncCallsExample::sync1)
			.thenParallelWithoutWait(AsyncCallsExample::doAsync1).then(AsyncCallsExample::doAsync2)
		.getParentTask()
		.then(AsyncCallsExample::sync2)
		.then(AsyncCallsExample::sync3)
		.then(AsyncCallsExample::sync4)
		.then(AsyncCallsExample::sync5)
		.start();
	}

	
	
	static void doAsync1() {
		System.out.println("doAsync1");
	}
	
	static void doAsync2() {
		System.out.println("doAsync2");
	}
	
	static void sync1() {
		System.out.println("sync1");
	}
	
	static void sync2() {
		System.out.println("sync2");
	}
	
	static void sync3() {
		System.out.println("sync3");
	}
	
	static void sync4() {
		System.out.println("sync4");
	}
	
	static void sync5() {
		System.out.println("sync5");
	}
	
}
