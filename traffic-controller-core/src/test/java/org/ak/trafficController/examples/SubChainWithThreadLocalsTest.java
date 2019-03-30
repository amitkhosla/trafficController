/**
 * 
 */
package org.ak.trafficController.examples;

import java.util.ArrayList;
import java.util.List;

import org.ak.trafficController.TaskExecutor;
import org.ak.trafficController.ThreadingDetails;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author amit.khosla
 *
 */
public class SubChainWithThreadLocalsTest {
	
	TaskExecutor executor = TaskExecutor.getInstance();
	
	List<String> chainFlow = new ArrayList<>();
	
	static ThreadLocal<String> threadLocal = new ThreadLocal<>();
	
	static void setThreadLocal(String val) {
		threadLocal.set(val);
	}
	
	static String getThreadLocal() {
		return threadLocal.get();
	}
	
	static void remove() {
		threadLocal.remove();
	}
	
	@Test
	public void testChaining() throws Throwable {
		parentTask();
		for (int i=1;i<12;i++) {
			System.out.println("verifying " + i);
			Assert.assertTrue(chainFlow.get(i-1).equals(i+"myString"));
		}
	}
	
	public void parentTask() throws Throwable {
		executor.of(()->chainFlow.add("1" + getThreadLocal()))
			.addThreadRelatedDetails(new ThreadingDetails<String>().setObjectFromMainFlow("myString").setCleaner(s->remove()).setProcessingForEachThread(s->setThreadLocal(s)))
			.then(()->chainFlow.add("2" + getThreadLocal()))
			.then(()->subTask1())
			.then(()->chainFlow.add("8" + getThreadLocal()))
			.then(()->subTask2()).startWithoutLettingExecutingThreadWait();;
	}

	/**
	 * @return
	 * @throws Throwable 
	 */
	private Object subTask2() throws Throwable {
		// TODO Auto-generated method stub
		executor.slowOf(()->chainFlow.add("9" + getThreadLocal())).then(()->subsubTask2()).startWithoutLettingExecutingThreadWait();;
		return null;
	}

	/**
	 * @return
	 * @throws Throwable 
	 */
	private Object subsubTask2() throws Throwable {
		// TODO Auto-generated method stub
		executor.of(()->chainFlow.add("10" + getThreadLocal())).thenSlow(()->chainFlow.add("11" + getThreadLocal())).startWithoutLettingExecutingThreadWait();
		return null;
	}

	/**
	 * @return
	 * @throws Throwable 
	 */
	private Object subTask1() throws Throwable {
		// TODO Auto-generated method stub
		executor.of(()->chainFlow.add("3" + getThreadLocal()))
			.then(()->chainFlow.add("4" + getThreadLocal()))
			.then(()->subsubtask())
			.then(()->chainFlow.add("7" + getThreadLocal()))
			.startWithoutLettingExecutingThreadWait();
		return null;
	}

	/**
	 * @return
	 * @throws Throwable 
	 */
	private Object subsubtask() throws Throwable {
		// TODO Auto-generated method stub
		executor.of(()->chainFlow.add("5" + getThreadLocal())).then(()->chainFlow.add("6" + getThreadLocal())).startWithoutLettingExecutingThreadWait();
		return null;
	}
}
