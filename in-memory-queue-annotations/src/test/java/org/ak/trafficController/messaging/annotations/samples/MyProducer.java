package org.ak.trafficController.messaging.annotations.samples;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.ak.trafficController.messaging.annotations.Queued;

@Named
public class MyProducer {
	//@Controlled
	@Queued(consumerClass=MyConsumer.class, consumerMethod="doSomething")
	public int produce(int k) {
		return k*5;
	}
	
	@Queued(itemInCollection=true, consumerClass=MyConsumer.class, consumerMethod="processNumberList", listConsumer=true)
	public List<Integer> produceList(int k) {
		ArrayList<Integer> list = new ArrayList<>();
		for (int i=0;i<5;i++) {
			list.add(i+k);
			list.add(25000+k+i);
			list.add(10000+k+i);
			list.add(250000+k+i);
			list.add(100000+k+i);
			list.add(2500000+k+i);
			list.add(1000000+k+i);
			list.add(2505000+k+i);
			list.add(1005000+k+i);
		}
		return list;
	}
	
	@Queued(consumerClass=MyConsumer.class, consumerMethod="doSomething")
	public MyOtherClass produceMyClass(int k) {
		return (MyOtherClass) new MyOtherClass().setSomeData(k*5);
	}
	
	@Queued(name="twice", consumerClass=MyProducer.class, consumerMethod="produceThrice")
	public Integer produceTwice(int k) {
		int output = k+2;
		System.out.println(output + Thread.currentThread().getName());
		return output;
	}
	
	@Queued(name="thrice", consumerClass=MyProducer.class, consumerMethod="produceTwice")
	public Integer produceThrice(int k) {
		int output = k+3;
		System.out.println(output + Thread.currentThread().getName());
		return output;
	}
	
}
