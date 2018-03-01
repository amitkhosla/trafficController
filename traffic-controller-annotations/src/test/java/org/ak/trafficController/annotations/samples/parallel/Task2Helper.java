package org.ak.trafficController.annotations.samples.parallel;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Join;

@Named
public class Task2Helper {

	@Join
	public Integer doSomething(Integer a) {
		System.out.println("Task2Helper is getting executed." + a);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task2 is executed."+a);
		return a *2; 
	}
	
	@Controlled
	public void logData(Integer a) {
		System.out.println("Task 2 helper log data is executing.");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task2 helper log is executed."+a);
		
	}
	
	@Controlled
	public Integer getDataBack(Integer a) {
		System.out.println("Task 2 helper get data is executing.");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Task2 helper get data is executed."+a);
		return a;
	}
}
