package org.ak.trafficController.annotations.samples.parallel;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Submit;

@Named
public class Task4 {
	@Controlled
	public void doSomething() {
		System.out.println("Task4 is getting executed.");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task4 is executed.");
	}

	@Controlled
	public Integer doSomething(int a) {
		// TODO Auto-generated method stub
		System.out.println("Task4 is getting executed." + a);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task4 is executed."+a);
		return a * 4;
	}

	@Submit(executorName="myExec", maxConsumer="1")
	public void async() {
		System.out.println("Async is called.");
	}
}
