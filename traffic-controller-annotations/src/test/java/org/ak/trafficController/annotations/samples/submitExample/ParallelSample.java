package org.ak.trafficController.annotations.samples.submitExample;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Submit;

@Named
public class ParallelSample {
	
	@Inject
	SubmitSampleLink submitSampleLink;
	
	public ParallelSample() {
		System.out.println("Service 1 constructor called");
	}
	
	@Controlled(maxConsumer="2", maxSlowConsumer="maxConsumer", executorName="myExecutor")
	public void doSomething() {
		System.out.println("I will do something.");
		submitSampleLink.doSomethingElse();
		submitSampleLink.anotherThing();
	}
	
	
	public int maxConsumer() {
		return 5;
	}
	
}
