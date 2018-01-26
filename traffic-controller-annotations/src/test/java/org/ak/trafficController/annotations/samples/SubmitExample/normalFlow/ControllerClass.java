package org.ak.trafficController.annotations.samples.SubmitExample.normalFlow;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;

@Named
public class ControllerClass {
	
	@Controlled
	public void doSomeOperation() {
		
	}
}
