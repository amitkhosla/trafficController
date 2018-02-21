package org.ak.trafficController.annotations.samples.parallel;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.annotations.api.Parallel;

@Named
public class ParallelClass {

	@Inject
	Task1 task1;
	
	@Inject
	Task2 task2;
	
	@Inject
	Task3 task3;
	
	@Inject
	Task4 task4;
	
	@Inject
	Joiner joiner;
	
	@Parallel
	public void doInParallel() {
		task1.doSomething();
		task2.doSomething();
		task3.doSomething();
		task4.doSomething();
	}
	
	@Parallel
	public Integer doInParallel(int a) {
		Integer i = task1.doSomething(a);
		Integer j = task2.doSomething(a);
		Integer k = task3.doSomething(a);
		Integer l = task4.doSomething(a);
		return joiner.join(i,j,k,l); 
	}
}
