package org.ak.trafficController.examples;

import org.ak.trafficController.TaskExecutor;

public class AnotherSampleProgramHandlingException {
	
	public static void main(String[] args) throws Throwable {
		way5();
		//way4();
		//way2();
	}
	
	protected static void way1() throws Throwable {
		try {
			TaskExecutor.getInstance()
			.of(AnotherSampleProgramHandlingException::doSomething)
			.then(AnotherSampleProgramHandlingException::throwException)
			.then(AnotherSampleProgramHandlingException::doSomethingElse).start();
			System.out.println("done...");
		} catch (Exception e) {
			System.out.println("exception caught");
			e.printStackTrace();//exception thrown from throwException will be handled here.
		}
	}


	protected static void way5() throws Throwable {
		try {
			System.out.println("way 3");
			TaskExecutor.getInstance()
			.of(AnotherSampleProgramHandlingException::doSomething)
			.then(AnotherSampleProgramHandlingException::throwException)
			.then(AnotherSampleProgramHandlingException::doSomethingElse).start();
			System.out.println("done...");
		} catch (Exception e) {
			System.out.println("exception caught");
			e.printStackTrace();//this should not throw any exception.
		}
	}
	
	protected static void way3() throws Throwable {
		try {
			System.out.println("way 3");
			TaskExecutor.getInstance()
			.of(AnotherSampleProgramHandlingException::doSomething)
			.then(AnotherSampleProgramHandlingException::throwException)
				.onException(Throwable::printStackTrace)
			.getParentTask().shouldContinueNextTaskIfExceptionOccurs().then(AnotherSampleProgramHandlingException::doSomethingElse).start();
			System.out.println("done...");
		} catch (Exception e) {
			System.out.println("exception caught");
			e.printStackTrace();//this should not throw any exception.
		}
	}
	
	protected static void way4() throws Throwable {
		try {
			System.out.println("way 4");
			TaskExecutor.getInstance()
			.of(AnotherSampleProgramHandlingException::doSomething)
			.then(AnotherSampleProgramHandlingException::throwException)
				.onException(Throwable::printStackTrace).then(()->"exception occured might have aborted").thenConsume(System.out::println)
			.getParentTask().shouldContinueNextTaskIfExceptionOccurs().then(AnotherSampleProgramHandlingException::doSomethingElse).submit();
			System.out.println("done...");//this should be printed way early.
		} catch (Exception e) {
			System.out.println("exception caught");
			e.printStackTrace();//this should not throw any exception.
		}
	}
	
	protected static void way2() throws Throwable {
		try {
			System.out.println("way 2");
			TaskExecutor.getInstance()
			.of(AnotherSampleProgramHandlingException::doSomething)
			.then(AnotherSampleProgramHandlingException::throwException)
				.onException(Throwable::printStackTrace)
			.getParentTask().then(AnotherSampleProgramHandlingException::doSomethingElse).start();
			System.out.println("done...");
		} catch (Exception e) {
			System.out.println("exception caught");
			e.printStackTrace();//this should not throw any exception.
		}
	}

	
	public static void doSomething() {
		System.out.println("doSomething");
	}
	
	public static void throwException() {
		System.out.println("throw exception");
		throw new RuntimeException("My dummy exception");
	}
	
	public static void doSomethingElse() {
		System.out.println("something else");
	}
}
