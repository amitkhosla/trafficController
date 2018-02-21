package org.ak.trafficController.annotations.samples.parallel;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ParallelFlowCaller {

	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(ParallelConfig.class);
		ParallelClass service1 = context.getBean(ParallelClass.class);
		System.out.println("value got..." + service1.doInParallel(234));
		System.out.println("hi");
		service1.doInParallel();
	}

}
