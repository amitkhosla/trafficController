package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.ak.Utils;
import org.junit.Assert;
import org.junit.Test;

public class GenericInMemoryQueueTest {

	@Test
	public void testAddItem(){
		GenericInMemoryQueue<Integer> memoryQueue = new GenericInMemoryQueue<>("someName");
		memoryQueue.add(2);
		Assert.assertTrue(memoryQueue.getNumberOfItemsInQueue().equals(1l));
		Queue queue = Utils.getValueFromObject(memoryQueue, "dataQueue");
		Assert.assertEquals(1, queue.size());
		memoryQueue.add(2);
		Assert.assertEquals(2, queue.size());
		Assert.assertTrue(memoryQueue.getNumberOfItemsInQueue().equals(2l));
	}
	
	@Test
	public void testAddItemAndTestRetrieval() {
		GenericInMemoryQueue<Integer> memoryQueue = new GenericInMemoryQueue<>("someName");
		memoryQueue.add(23);
		StringBuilder sb = new StringBuilder();
		memoryQueue.register(i->sb.append(i), "some consumer");
		Utils.sleep(10l);
		Assert.assertEquals("23", sb.toString());
		List<Integer> list = new ArrayList<>();
		list.add(4);list.add(5);
		memoryQueue.add(list); //lets see if add to list works
		Utils.sleep(10l);
		Assert.assertEquals("2345", sb.toString());
		Assert.assertEquals(1, memoryQueue.getDirectConsumerCount());
		Assert.assertEquals(0, memoryQueue.getBatchConsumerCount());
		memoryQueue.shutdown();
		Utils.sleep(10l);
		Assert.assertEquals(0, memoryQueue.getDirectConsumerCount());
		Assert.assertEquals(0, memoryQueue.getBatchConsumerCount());
		
	}
	
	@Test
	public void testAddItemAndTestListRetrieval() {
		GenericInMemoryQueue<Integer> memoryQueue = new GenericInMemoryQueue<>("someName", 10);
		AtomicInteger calledCount = new AtomicInteger(0);
		List<Integer> outputlist = new ArrayList<>();
		memoryQueue.registerBatchConsumer(i->{
			outputlist.addAll(i);
			calledCount.incrementAndGet();
		}, "some consumer");
		List<Integer> list = new ArrayList<>();
		for (int i=0;i<105;i++) {
		list.add(i);
		}
		memoryQueue.add(list); //lets see if add to list works
		Utils.sleep(10l);
		Assert.assertEquals(11, calledCount.get());
		for (int i=0;i<105;i++) {
			Assert.assertTrue(outputlist.contains(i));
		}
		Assert.assertEquals(0, memoryQueue.getDirectConsumerCount());
		Assert.assertEquals(1, memoryQueue.getBatchConsumerCount());
		
		memoryQueue.shutdown();
		Utils.sleep(10l);
		Assert.assertEquals(0, memoryQueue.getDirectConsumerCount());
		Assert.assertEquals(0, memoryQueue.getBatchConsumerCount());
		
	}

}
