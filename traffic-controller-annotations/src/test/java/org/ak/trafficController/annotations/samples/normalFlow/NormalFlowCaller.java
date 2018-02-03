package org.ak.trafficController.annotations.samples.normalFlow;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class NormalFlowCaller {

	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigClassNormal.class);
		ControllerClass service1 = context.getBean(ControllerClass.class);
		service1.doSomeOperation();
		
	}

}
