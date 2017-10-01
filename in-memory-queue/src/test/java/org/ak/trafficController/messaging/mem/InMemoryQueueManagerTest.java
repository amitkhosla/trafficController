package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.ak.Utils;
import org.junit.Assert;
import org.junit.Test;


public class InMemoryQueueManagerTest {

	@Test
	public void testInitialize() {
		String queueName="queueName";
		List<Integer> list = new ArrayList<>();
		Consumer<Integer> consumer = i->list.add(i);
		int numberOfConsumers = 2;
		InMemoryQueueManager inMemoryQueueManager = new InMemoryQueueManager();
		inMemoryQueueManager.initialize(queueName, consumer, numberOfConsumers);
		inMemoryQueueManager.add(queueName, 234);
		Utils.sleep(50);
		Assert.assertTrue(list.contains(234));
		int expected = 2;
		Assert.assertEquals(expected, inMemoryQueueManager.getDirectConsumerCount(queueName).intValue());
		inMemoryQueueManager.setConsumerCount(queueName, 1);
		Assert.assertEquals(1, inMemoryQueueManager.getDirectConsumerCount(queueName).intValue());
	}
	
}
