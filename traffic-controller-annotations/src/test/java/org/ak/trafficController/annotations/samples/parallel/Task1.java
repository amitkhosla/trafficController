package org.ak.trafficController.annotations.samples.parallel;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;

@Named
public class Task1 {
	@Controlled
	public void doSomething() {
		System.out.println("Task1 is getting executed.");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task1 is executed.");
	}
	
	@Controlled
	public Integer doSomething(int a) {
		System.out.println("Task1 is getting executed with value ... ." + a);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task1 is executed. with value..." + a);
		return a ;
	}
}
