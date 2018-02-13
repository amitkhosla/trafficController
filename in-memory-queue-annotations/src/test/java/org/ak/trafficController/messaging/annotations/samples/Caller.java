package org.ak.trafficController.messaging.annotations.samples;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Caller {
	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		MyConsumer consumer = context.getBean(MyConsumer.class);
		MyProducer service1 = context.getBean(MyProducer.class);
		//service1.produceMyClass(2);
		//consumer.processNumberList(new ArrayList<>());
		//consumer.processNumber(0);
		/*for (int i=0;i<100;i++) {
			service1.produceList(i*5);
			service1.produce(i*5);
		}*/
		service1.produceTwice(1);
	}
	
	
}
