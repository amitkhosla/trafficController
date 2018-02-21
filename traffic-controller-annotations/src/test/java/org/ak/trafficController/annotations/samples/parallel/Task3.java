package org.ak.trafficController.annotations.samples.parallel;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;

@Named
public class Task3 {
	@Controlled
	public void doSomething() {
		System.out.println("Task3 is getting executed.");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task3 is executed.");
	}

	@Controlled
	public Integer doSomething(int a) {
		// TODO Auto-generated method stub
		System.out.println("Task3 is getting executed." + a);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task3 is executed." +a);
		return a*3;
		
	}
}
