package org.ak.trafficController;

/**
 * A runnable which can also throw exception.
 * Java runnable could be used in most of the cases but where any method throws any exception, we need to handle the exception there itself.
 * As we also have option to attach exception handlers, we need to overcome this. 
 * @author amit.khosla
 *
 */
@FunctionalInterface
public interface RunnableToBeExecuted {
	/**
	 * Runs the task which we want to execute.
	 * @throws Throwable Throws in case any exception occurs
	 */
	public void run() throws Throwable;
}
