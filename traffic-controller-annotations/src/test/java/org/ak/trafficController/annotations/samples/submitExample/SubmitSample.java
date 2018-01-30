package org.ak.trafficController.annotations.samples.submitExample;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Submit;

@Named
public class SubmitSample {
	
	@Inject
	SubmitSampleLink submitSampleLink;
	
	public SubmitSample() {
		System.out.println("Service 1 constructor called");
	}
	
	//@Parallel(maxConsumer="2", maxSlowConsumer="maxConsumer", executorName="myExecutor")
	@Submit(maxConsumer="2", maxSlowConsumer="maxConsumer", executorName="myExecutor")
	public void doSomething() {
		System.out.println("I will do something.");
		submitSampleLink.doSomethingElse();
		submitSampleLink.anotherThing();
	}
	
	
	public int maxConsumer() {
		return 5;
	}
	
}
