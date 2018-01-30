package org.ak.trafficController.annotations.samples.normalFlow;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;

@Named
public class DataWorkerService {
	@Controlled
	public void doSomeThingWithData(String key, double value) {
		System.out.println("doing something on data");
	}
	
}
