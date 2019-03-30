/**
 * 
 */
package org.ak.trafficController.examples;

import java.util.ArrayList;
import java.util.List;

import org.ak.trafficController.TaskExecutor;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author amit.khosla
 *
 */
public class SubChainTest {
	
	TaskExecutor executor = TaskExecutor.getInstance();
	
	List<String> chainFlow = new ArrayList<>();
	
	@Test
	public void testChaining() throws Throwable {
		parentTask();
		for (int i=1;i<12;i++) {
			System.out.println("verifying " + i);
			Assert.assertTrue(chainFlow.get(i-1).equals(i+""));
		}
	}
	
	public void parentTask() throws Throwable {
		executor.of(()->chainFlow.add("1"))
			.then(()->chainFlow.add("2"))
			.then(()->subTask1())
			.then(()->chainFlow.add("8"))
			.then(()->subTask2()).startWithoutLettingExecutingThreadWait();;
	}

	/**
	 * @return
	 * @throws Throwable 
	 */
	private Object subTask2() throws Throwable {
		// TODO Auto-generated method stub
		executor.slowOf(()->chainFlow.add("9")).then(()->subsubTask2()).startWithoutLettingExecutingThreadWait();;
		return null;
	}

	/**
	 * @return
	 * @throws Throwable 
	 */
	private Object subsubTask2() throws Throwable {
		// TODO Auto-generated method stub
		executor.of(()->chainFlow.add("10")).thenSlow(()->chainFlow.add("11")).startWithoutLettingExecutingThreadWait();
		return null;
	}

	/**
	 * @return
	 * @throws Throwable 
	 */
	private Object subTask1() throws Throwable {
		// TODO Auto-generated method stub
		executor.of(()->chainFlow.add("3"))
			.then(()->chainFlow.add("4"))
			.then(()->subsubtask())
			.then(()->chainFlow.add("7"))
			.startWithoutLettingExecutingThreadWait();
		return null;
	}

	/**
	 * @return
	 * @throws Throwable 
	 */
	private Object subsubtask() throws Throwable {
		// TODO Auto-generated method stub
		executor.of(()->chainFlow.add("5")).then(()->chainFlow.add("6")).startWithoutLettingExecutingThreadWait();
		return null;
	}
}
