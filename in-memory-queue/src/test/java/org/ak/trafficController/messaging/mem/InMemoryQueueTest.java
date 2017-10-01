package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.ak.Utils;
import org.junit.Test;

import junit.framework.Assert;

public class InMemoryQueueTest {
	
	@Test
	public void testInitialize() {
		String queueName = "queueName"; 
		ConcurrentLinkedQueue<Integer> retrievedFromQueue = new ConcurrentLinkedQueue<>();
		Consumer<Integer> consumer = i-> retrievedFromQueue.add(i);
		Consumer<List<Integer>> batchConsumer = i-> retrievedFromQueue.addAll(i);
		InMemoryQueue<Integer> imq = testDefaultSettings(queueName, consumer, batchConsumer);
		testCountSetProperly(imq);

		testSingleAddWorking(retrievedFromQueue, imq);
		
		testListOFItemsAddedProperly(retrievedFromQueue, imq);
		testShutdown(imq);
	}

	protected void testSingleAddWorking(ConcurrentLinkedQueue<Integer> retrievedFromQueue, InMemoryQueue<Integer> imq) {
		imq.add(1);
		Utils.sleep(5);
		Assert.assertTrue("1 should be present but found missing",retrievedFromQueue.contains(1));
	}

	protected void testListOFItemsAddedProperly(ConcurrentLinkedQueue<Integer> retrievedFromQueue,
			InMemoryQueue<Integer> imq) {
		List<Integer> input = new ArrayList<>();
		for (int i=100;i<1000;i++) {
			input.add(i);
		}
		imq.addAllFromList(input);
		Utils.sleep(5);
		for (int i=100;i<1000;i++) {
			Assert.assertTrue(i+"should be present but found missing",retrievedFromQueue.contains(i));
		}
	}

	protected void testCountSetProperly(InMemoryQueue<Integer> imq) {
		imq
				.setDirectConsumerCount(3)
				.setBatchConsumerCount(4);
		Assert.assertEquals(4, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(3, imq.inMemoryQueue.getDirectConsumerCount());
		Assert.assertEquals(4, imq.getBatchConsumerCount().intValue());
		Assert.assertEquals(3, imq.getDirectConsumerCount().intValue());
		
		imq.incrementDirectConsumer();
		Assert.assertEquals(4, imq.inMemoryQueue.getDirectConsumerCount());
		Assert.assertEquals(4, imq.getDirectConsumerCount().intValue());
		
		imq.decrementBatchConsumer();
		Assert.assertEquals(3, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(3, imq.getBatchConsumerCount().intValue());
		

		imq.decrementDirectConsumer();
		Assert.assertEquals(3, imq.inMemoryQueue.getDirectConsumerCount());
		Assert.assertEquals(3, imq.getDirectConsumerCount().intValue());
		
		imq.incrementBatchConsumer();
		Assert.assertEquals(4, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(4, imq.getBatchConsumerCount().intValue());
		
		imq.setDirectConsumerCount(10);
		imq.setBatchConsumerCount(10);
		Assert.assertEquals(10, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(10, imq.getBatchConsumerCount().intValue());
		
		imq.setDirectConsumerCount(1);
		imq.setBatchConsumerCount(1);
		Assert.assertEquals(1, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(1, imq.getBatchConsumerCount().intValue());
		
		imq.setDirectConsumerCount(0);
		imq.setBatchConsumerCount(0);
		Assert.assertEquals(0, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(0, imq.getBatchConsumerCount().intValue());
		
		imq.setDirectConsumerCount(4);
		imq.setBatchConsumerCount(4);
		Assert.assertEquals(4, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(4, imq.getBatchConsumerCount().intValue());
	}

	protected InMemoryQueue<Integer> testDefaultSettings(String queueName, Consumer<Integer> consumer,
			Consumer<List<Integer>> batchConsumer) {
		InMemoryQueue<Integer> imq = new InMemoryQueue<Integer>(queueName)
				.setDirectConsumer(consumer)
				.setBatchConsumer(batchConsumer);
		Assert.assertEquals(1, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(1, imq.inMemoryQueue.getDirectConsumerCount());
		return imq;
	}

	protected void testShutdown(InMemoryQueue<Integer> imq) {
		imq.shutdown();
		Assert.assertEquals(0, imq.inMemoryQueue.getBatchConsumerCount());
		Assert.assertEquals(0, imq.inMemoryQueue.getDirectConsumerCount());
	}
	
	@Test
	public void testItemsAddedProperly() {
		InMemoryQueue<Integer> imq = new InMemoryQueue<>("queueName");
		imq.setBatchSize(100);
		for (int i=1000;i<10000;i++) {
			imq.add(i);
		}
		Assert.assertEquals(9000, imq.inMemoryQueue.getNumberOfItemsInQueue().intValue());
		Assert.assertEquals(9000, imq.getNumberOfItemsInQueue().intValue());
		AtomicBoolean ab = new AtomicBoolean(true);
		AtomicInteger ai = new AtomicInteger(0);
		imq.setBatchConsumer(list-> {
			if(ab.getAndSet(false)){
				ai.set(list.size());
			}
		});
		Utils.sleep(100);
		Assert.assertEquals(100, ai.get());
		imq.shutdown();
	}
	
	@Test
	public void testClean() {
		InMemoryQueue<Integer> imq = new InMemoryQueue<Integer>("queueName") ;
		for (int i = 0; i< 100; i++) {
			imq.add(i);
		}
		imq.clear(null);
		Assert.assertEquals(0, imq.getNumberOfItemsInQueue().intValue());
		List<Integer> list1 = new ArrayList<>();
		List<Integer> list2 = new ArrayList<>();
		Consumer<Queue<Integer>> consumer1  = queue-> list1.addAll(queue);
		Consumer<Queue<Integer>> consumer2  = queue-> list2.addAll(queue);
		for (int i = 0; i< 100; i++) {
			imq.add(i);
		}
		imq.clear(consumer1, consumer2);
		Assert.assertEquals(0, imq.getNumberOfItemsInQueue().intValue());
		for (int i=0; i <100; i++) {
			Assert.assertTrue(list1.contains(i));
			Assert.assertTrue(list2.contains(i));
		}
		Assert.assertFalse(imq.isBatchConsumerSet());
		Assert.assertFalse(imq.isDirectConsumerSet());
	}
	
}
