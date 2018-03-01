package org.ak.trafficController.annotations.samples.parallel;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Parallel;

@Named
public class Task2 {
	
	@Inject
	Task2Helper task2Helper;
	
	@Controlled
	public void doSomething() {
		System.out.println("Task2 is getting executed.");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task2 is executed.");
	}

	@Parallel
	public Integer doSomething(int a) {
		task2Helper.logData(a);
		Integer data = task2Helper.getDataBack(a);
		return task2Helper.doSomething(data);
	}
	
	
}
