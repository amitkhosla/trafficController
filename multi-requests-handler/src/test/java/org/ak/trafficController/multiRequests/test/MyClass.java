/**
 * 
 */
package org.ak.trafficController.multiRequests.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Named;

import org.ak.trafficController.multiRequests.ControlMultiRequest;

/**
 * @author amit.khosla
 *
 */
@Named
public class MyClass {
	
	@ControlMultiRequest(reusePreviousRunResult=100)
	public int doubleMe(int number) {
		try {
			Thread.sleep(1000l); //to add custom delay
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return number * 2;
	}
	
	@ControlMultiRequest(reusePreviousRunResult=100)
	public int doSomeThing() {
		try {
			Thread.sleep(1000l); //to add custom delay
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 20;
	}
	
	@ControlMultiRequest(reusePreviousRunResult=100)
	public int doSomeThingElse() {
		try {
			Thread.sleep(1000l); //to add custom delay
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 40;
	}
	
	AtomicInteger ai = new AtomicInteger(0);
	
	@ControlMultiRequest(reusePreviousRunResult=100)
	public int returnNextNumber() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ai.incrementAndGet();
	}
	
	static ConcurrentLinkedQueue<List<Integer>> clq = new ConcurrentLinkedQueue<>();
	static ConcurrentHashMap<Integer, AtomicInteger> cMap = new ConcurrentHashMap<>();
	
	static {
		for (int i=0; i<1000;i++) {
			cMap.put(i, new AtomicInteger());
		}
	}
	
	@ControlMultiRequest(reusePreviousRunResult=100, shouldConsiderPartial=true)
	public Map<Integer, Integer> getDouble(List<Integer> list) {
		Map<Integer, Integer> map = new HashMap<>();
		list.forEach(l->{
			cMap.get(l).incrementAndGet();
			map.put(l, l*2);
		});
		try {
			Thread.sleep(1000l);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}
	
	@ControlMultiRequest(reusePreviousRunResult=100, shouldConsiderPartial=true)
	public Map<Integer, Double> getHalfForAll(List<Integer> list) {
		clq.add(new ArrayList<>(list));
		Map<Integer, Double> map = new HashMap<>();
		list.forEach(i->map.put(i, ((double)i)/2));
		return map;
		
	}
}
