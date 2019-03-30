/**
 * 
 */
package org.ak.trafficController.examples;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.ak.trafficController.TaskExecutor;

/**
 * @author amit.khosla
 *
 */
public class SubChainParallelTest {

	TaskExecutor executor = TaskExecutor.getInstance();
	ConcurrentLinkedQueue<String> clq = new ConcurrentLinkedQueue<>();
	
	public void parentParallel() {
		//executor.of(()->"Some String").thenConsumeMultiple(this::s1, this::s2, this::s3, this::s4);
	}
	
	private void s1(String s) {
		clq.add("s1");
		//executor.sl
	}
}
