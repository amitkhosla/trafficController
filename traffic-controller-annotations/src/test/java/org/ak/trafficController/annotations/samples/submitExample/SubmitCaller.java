package org.ak.trafficController.annotations.samples.submitExample;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SubmitCaller {
	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigClass.class);
		SubmitSample service1 = context.getBean(SubmitSample.class);
		service1.doSomething();
	}
}
