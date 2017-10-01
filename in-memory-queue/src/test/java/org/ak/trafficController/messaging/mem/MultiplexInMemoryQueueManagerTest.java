package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.ak.Utils;
import org.junit.Test;

import junit.framework.Assert;

public class MultiplexInMemoryQueueManagerTest {
	@Test
	public void testAdd() {
		List<Integer> integers1 = new ArrayList<>();
		List<Double> doubles1 = new ArrayList<>();
		List<String> strings1 = new ArrayList<>();
		List<Integer> integers2 = new ArrayList<>();
		List<Double> doubles2 = new ArrayList<>();
		List<String> strings2 = new ArrayList<>();
		Consumer<Integer> intconsumer1 = i->integers1.add(i);
		Consumer<Double> doubleconsumer1 = i->doubles1.add(i);
		Consumer<String> stringConsumer1 = i->strings1.add(i);
		Consumer<Integer> intconsumer2 = i->integers2.add(i);
		Consumer<Double> doubleconsumer2 = i->doubles2.add(i);
		Consumer<String> stringConsumer2 = i->strings2.add(i);
		
		MultiplexInMemoryQueueManager manager = new MultiplexInMemoryQueueManager();
		manager.register("int1", intconsumer1);
		manager.register("int2", intconsumer2);
		manager.register("double1", doubleconsumer1);
		manager.register("double2", doubleconsumer2);
		manager.register("strings1", stringConsumer1);
		manager.register("strings2", stringConsumer2);
		manager.add("int1",1);
		List<Integer> list = new ArrayList<>();
		list.add(2);
		list.add(3);
		manager.addMultiple("int2", list );
		
		
		manager.add("double1", 23.0);
		manager.add("double2", 2.0);
		manager.add("strings1", "abcd");
		manager.add("strings2", "xyz");

		Utils.sleep(10);
		
		Assert.assertTrue(integers1.contains(1));
		
		Assert.assertTrue(integers2.contains(2));
		Assert.assertTrue(integers2.contains(3));
		Assert.assertTrue(doubles1.contains(23.0));
		Assert.assertTrue(doubles2.contains(2.0));
		
		Assert.assertTrue(strings1.contains("abcd"));
		Assert.assertTrue(strings2.contains("xyz"));
	}
	
	@Test
	public void testAddWhenAddedBeforeRegistering() {
		List<Integer> integers1 = new ArrayList<>();
		Consumer<Integer> intconsumer1 = i->integers1.add(i);
		MultiplexInMemoryQueueManager manager = new MultiplexInMemoryQueueManager();
		for (int i=0;i<1000;i++) {
			manager.add("integer", i);
		}
		manager.register("integer", intconsumer1);
		
		Utils.sleep(10);
		for (int i=0;i<1000;i++) {
			Assert.assertTrue(integers1.contains(i));
		}
	}
}
