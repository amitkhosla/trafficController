package org.ak.trafficController.messaging.annotations.samples;

import java.util.Collection;

import javax.inject.Named;

import org.ak.trafficController.messaging.annotations.Consumer;

@Named
public class MyConsumer {
	@Consumer(numberOfConsumers=2)
	public void processNumber(int number) {
		System.out.println(number + " processed from " + Thread.currentThread().getName() );
	}
	
	@Consumer(batch=true, numberOfConsumers=2)
	public void processNumberList(Collection<Integer> numbers) {
		System.out.println(numbers + " processed from " + Thread.currentThread().getName() );
	}
}
