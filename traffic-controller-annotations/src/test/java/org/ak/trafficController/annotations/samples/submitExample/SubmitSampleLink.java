package org.ak.trafficController.annotations.samples.submitExample;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Submit;
import org.ak.trafficController.annotations.api.TaskType;

@Named
public class SubmitSampleLink {

	@Controlled(maxConsumer="2", maxSlowConsumer="maxConsumer", executorName="myExecutor")
	//@Submit(maxConsumer="2", maxSlowConsumer="maxConsumer", executorName="myExecutor")
	public void doSomethingElse() {
		System.out.println("I will do something else.");
		throw new RuntimeException("some runtime exception.");
	}

	@Submit(maxConsumer="2", maxSlowConsumer="maxConsumer", executorName="myExecutor", taskType=TaskType.SLOW)
	public void anotherThing() {
		System.out.println("I will be doing something else.");
	}
	
	public int maxConsumer() {
		return 5;
	}
}
