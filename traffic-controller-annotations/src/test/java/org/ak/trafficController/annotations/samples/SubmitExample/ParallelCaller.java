package org.ak.trafficController.annotations.samples.SubmitExample;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ParallelCaller {
	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigClass.class);
		ParallelSample service1 = context.getBean(ParallelSample.class);
		service1.doSomething();
	}
}
