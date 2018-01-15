package org.ak.trafficController.examples;

import org.ak.trafficController.TaskExecutor;

public class AnotherSampleProgramHandlingException {
	
	public static void main(String[] args) throws Throwable {
		way1();
		way2();
	}
	
	protected static void way1() throws Throwable {
		try {
			TaskExecutor.getInstance()
			.of(AnotherSampleProgramHandlingException::doSomething)
			.then(AnotherSampleProgramHandlingException::throwException)
			.then(AnotherSampleProgramHandlingException::doSomethingElse).start();

		} catch (Exception e) {
			e.printStackTrace();//exception thrown from throwException will be handled here.
		}
	}


	protected static void way2() throws Throwable {
		try {
			TaskExecutor.getInstance()
			.of(AnotherSampleProgramHandlingException::doSomething)
			.then(AnotherSampleProgramHandlingException::throwException)
				.onException(Throwable::printStackTrace)
			.getParentTask().then(AnotherSampleProgramHandlingException::doSomethingElse).start();

		} catch (Exception e) {
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
