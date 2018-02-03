package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.ak.trafficController.messaging.exception.ThresholdReachedException;
import org.junit.Assert;
import org.junit.Test;

public class DynamicSettingsTest {

	@Test
	public void testAdjust() {
		StringBuilder sb = new StringBuilder();
		DynamicSettings<Integer> dynamicSettings = new DynamicSettings<Integer>(){
			@Override
			protected void increaseConsumerIfPossible() {
				sb.append("increaseConsumerIfPossible called");
			}
			@Override
			protected void tryIfCanFreeUp() {
				sb.append("tryIfCanFreeUp called");
			}
			
			@Override
			protected void decreaseConsumerIfPossible() {
				sb.append("decreaseConsumerIfPossible called");
			}
		};
		InMemoryQueue<Integer> queue = new InMemoryQueue<>("myQueue");
		dynamicSettings.setQueue(queue);
		dynamicSettings.adjust();
		Assert.assertEquals("decreaseConsumerIfPossible called", sb.toString());
		
		List<Integer> items = new ArrayList<>();
		for (int i=0;i<1000; i++) {
			items.add(i);
		}
		queue.addAllFromList(items);
		sb.setLength(0);
		dynamicSettings.adjust();
		String output = sb.toString();
		Assert.assertTrue(output.contains("increaseConsumerIfPossible called"));
		Assert.assertTrue(output.contains("tryIfCanFreeUp called"));
	}
	
	@Test
	public void testIncreaseDecreaseIfPossible() {
		InMemoryQueue<Integer> queue = new InMemoryQueue<Integer>("myQueue");
		DynamicSettings<Integer> dynamicSettings = new DynamicSettings<Integer>()
				.setQueue(queue).setMaxDirectConsumer(4).setMaxBatchConsumer(4);
		
		Supplier<Integer> directSupplier = ()->queue.inMemoryQueue.getDirectConsumerCount();
		Supplier<Integer> batchSupplier = ()->queue.inMemoryQueue.getBatchConsumerCount();

		
		List<Supplier<Integer>> changingSuppliers = new ArrayList<>();
		List<Supplier<Integer>> zeroSuppliers = new ArrayList<>();

		testNoConsumer(queue, dynamicSettings, directSupplier, batchSupplier, changingSuppliers, zeroSuppliers);
		
		
		queue.setDirectConsumer(i->{});
		
		testOnlyDirectSupplier(queue, dynamicSettings, directSupplier, batchSupplier, changingSuppliers, zeroSuppliers);
		
		testBothSet(queue, dynamicSettings, batchSupplier, changingSuppliers, zeroSuppliers);
		
		queue.removeDirectConsumer();
		testOnlyBatch(queue, dynamicSettings, changingSuppliers, zeroSuppliers);
		
		queue.shutdown();
	}

	protected void testOnlyBatch(InMemoryQueue<Integer> queue, DynamicSettings<Integer> dynamicSettings,
			List<Supplier<Integer>> changingSuppliers, List<Supplier<Integer>> zeroSuppliers) {
		dynamicSettings.setMaxBatchConsumer(20);
		zeroSuppliers.add(changingSuppliers.remove(0));
		testDirectConsumerSet(queue, dynamicSettings, changingSuppliers, zeroSuppliers, dynamicSettings.getMaxBatchConsumer());
	}

	protected void testBothSet(InMemoryQueue<Integer> queue, DynamicSettings<Integer> dynamicSettings,
			Supplier<Integer> batchSupplier, List<Supplier<Integer>> changingSuppliers,
			List<Supplier<Integer>> zeroSuppliers) {
		queue.setBatchConsumer(i->{});
		changingSuppliers.add(batchSupplier);
		zeroSuppliers.clear();
		testDirectConsumerSet(queue, dynamicSettings, changingSuppliers, zeroSuppliers, dynamicSettings.getMaxDirectConsumer());
	}

	protected void testOnlyDirectSupplier(InMemoryQueue<Integer> queue, DynamicSettings<Integer> dynamicSettings,
			Supplier<Integer> directSupplier, Supplier<Integer> batchSupplier,
			List<Supplier<Integer>> changingSuppliers, List<Supplier<Integer>> zeroSuppliers) {
		changingSuppliers.clear();
		zeroSuppliers.clear();
		changingSuppliers.add(directSupplier);
		zeroSuppliers.add(batchSupplier);
		testDirectConsumerSet(queue, dynamicSettings, changingSuppliers, zeroSuppliers, dynamicSettings.getMaxDirectConsumer());
	}

	protected void testNoConsumer(InMemoryQueue<Integer> queue, DynamicSettings<Integer> dynamicSettings,
			Supplier<Integer> directSupplier, Supplier<Integer> batchSupplier,
			List<Supplier<Integer>> changingSuppliers, List<Supplier<Integer>> zeroSuppliers) {
		zeroSuppliers.add(batchSupplier);
		zeroSuppliers.add(directSupplier);
		
		testDirectConsumerSet(queue, dynamicSettings, changingSuppliers, zeroSuppliers, dynamicSettings.getMaxDirectConsumer());
	}

	protected void testDirectConsumerSet(InMemoryQueue<Integer> queue, DynamicSettings<Integer> dynamicSettings, List<Supplier<Integer>> changingSuppliers, List<Supplier<Integer>> zeroSuppliers, int maxCount) {
		for (int i=2;i<=maxCount;i++) {
			dynamicSettings.increaseConsumerIfPossible();
			testValuesAsPerExpectation(changingSuppliers, zeroSuppliers, i);
		}
		dynamicSettings.increaseConsumerIfPossible();
		testValuesAsPerExpectation(changingSuppliers, zeroSuppliers, maxCount);
		dynamicSettings.increaseConsumerIfPossible();
		testValuesAsPerExpectation(changingSuppliers, zeroSuppliers, maxCount);
		
		for (int i=maxCount-1;i>0;i--) {
			dynamicSettings.decreaseConsumerIfPossible();
			testValuesAsPerExpectation(changingSuppliers, zeroSuppliers, i);
		}
		
		dynamicSettings.decreaseConsumerIfPossible();
		testValuesAsPerExpectation(changingSuppliers, zeroSuppliers, 1);
		dynamicSettings.decreaseConsumerIfPossible();
		testValuesAsPerExpectation(changingSuppliers, zeroSuppliers, 1);
	}

	protected void testValuesAsPerExpectation(List<Supplier<Integer>> changingSuppliers,
			List<Supplier<Integer>> zeroSuppliers, int expected) {
		for (Supplier<Integer> supplier : changingSuppliers) {
			Assert.assertEquals(expected, supplier.get().intValue());
		}
		for (Supplier<Integer> supplier : zeroSuppliers) {
			Assert.assertEquals(0, supplier.get().intValue());
		}
	}
	
	@Test
	public void testAddStillAllowed() {
		InMemoryQueue<Integer> queue = new InMemoryQueue<>("myQueue");
		AtomicBoolean handleStopAddingAtThreshold = new AtomicBoolean();
		DynamicSettings<Integer> ds = new DynamicSettings<Integer>(){
			protected boolean handleStopAddingAtThreshold() {
				return handleStopAddingAtThreshold.get();
			};
		}.setQueue(queue );
		Assert.assertTrue(ds.addStillAllowed());
		for(int i=0;i<=ds.getThresholdWhenNoMoreItemsShouldBeHandled();i++) {
			queue.add(i);
		}
		ds.setShouldThrowExceptionWhenThresholdAtAdd(true);
		handleStopAddingAtThreshold.set(true);
		ds.setShouldStopAddingAtThreshold(true);
		try {
			ds.addStillAllowed();
			Assert.fail("Should have thrown exception");
		} catch (ThresholdReachedException exception) {
			Assert.assertEquals("Thredhold limit reached. Cannot add more.", exception.getMessage());
		}
		
		ds.setShouldThrowExceptionWhenThresholdAtAdd(false);
		Assert.assertTrue(ds.addStillAllowed());
		
		handleStopAddingAtThreshold.set(false);
		Assert.assertFalse(ds.addStillAllowed());
		
		ds.setShouldStopAddingAtThreshold(false);
		Assert.assertTrue(ds.addStillAllowed());
		
	}
	
	@Test
	public void testAdd() {
		AtomicBoolean atomicBoolean = new AtomicBoolean();
		InMemoryQueue<Integer> queue = new InMemoryQueue<>("");
		DynamicSettings<Integer> ds = new DynamicSettings<Integer>() {
			@Override
			protected boolean addStillAllowed() {
				return atomicBoolean.get();
			}
		}.setQueue(queue );
		ds.addItemInQueue(2);
		Assert.assertEquals(0, queue.getNumberOfItemsInQueue().intValue());
		
		List<Integer> items = new ArrayList<>();
		items.add(2);
		ds.addItemsInQueue(items);
		Assert.assertEquals(0, queue.getNumberOfItemsInQueue().intValue());
		
		atomicBoolean.set(true);
		ds.addItemInQueue(2);
		Assert.assertEquals(1, queue.getNumberOfItemsInQueue().intValue());
	}
	
	@Test
	public void testwaitForRetriesIfAny() {
		InMemoryQueue<Integer> queue = new InMemoryQueue<>("");
		DynamicSettings<Integer> ds = new DynamicSettings<Integer>().setQueue(queue);
		Assert.assertTrue(ds.waitForRetriesIfAny());
		
		for (int i=0;i<10;i++) {
			for (int j=0;j<ds.getThresholdWhenNoMoreItemsShouldBeHandled(); j++) {
				queue.add(i);
			}
		}
		ds.setWaitForRetryThresholdLimit(1L);
		Assert.assertFalse(ds.waitForRetriesIfAny());
		
		queue.setDirectConsumer(i->{});
		Assert.assertTrue(ds.waitForRetriesIfAny());
	}
	
	@Test
	public void testhandleStopAddingAtThreshold() {
		InMemoryQueue<Integer> queue = null;
		AtomicBoolean ab = new AtomicBoolean();
		DynamicSettings<Integer> ds = new DynamicSettings<Integer>(){
			@Override
			protected boolean waitForRetriesIfAny() {
				// TODO Auto-generated method stub
				return ab.get();
			}
		};
		Assert.assertFalse(ds.handleStopAddingAtThreshold());
		
		ds.setShouldRetrySenderTillThresholdNotRecovered(true);
		Assert.assertFalse(ds.handleStopAddingAtThreshold());
		ab.set(true);
		Assert.assertTrue(ds.handleStopAddingAtThreshold());
		ab.set(false);
		ds.setShouldThrowExceptionPostRetry(true);
		try {
			ds.handleStopAddingAtThreshold();
			Assert.fail();
		} catch (ThresholdReachedException exception) {
			Assert.assertEquals("Threshold Reached and after waits also, cannot recover.", exception.getMessage());
		}
	}
	
	@Test
	public void testtryIfCanFreeUp() {
		AtomicLong numberOfItems = new AtomicLong();
		AtomicReference<Consumer<Queue<Integer>>> reference = new AtomicReference<Consumer<Queue<Integer>>>(null);
		StringBuilder sb = new StringBuilder();
		InMemoryQueue<Integer> queue = new InMemoryQueue<Integer>("") {
			@Override
			public Long getNumberOfItemsInQueue() {
				return numberOfItems.get();
			}
			
			@Override
			protected void clear(Consumer<Queue<Integer>>... consumer) {
				// TODO Auto-generated method stub
				sb.append("clear called");
				if (consumer!=null && consumer.length > 0) {
					reference.set(consumer[0]);
				}
			}
		};
		DynamicSettings<Integer> settings = new DynamicSettings<Integer>().setQueue(queue);
		settings.tryIfCanFreeUp();
		
		Assert.assertTrue(sb.indexOf("clear called") < 0);
		numberOfItems.set(100000L);
		
		settings.tryIfCanFreeUp();
		Assert.assertTrue(sb.indexOf("clear called") < 0);

		settings.setShouldClearQueueAtThreshold(true);
		Consumer<Queue<Integer>> consumer = q->{};
		settings.addPreCleanHandler(consumer );
		settings.tryIfCanFreeUp();
		Assert.assertTrue(sb.indexOf("clear called") >= 0);
		Assert.assertEquals(consumer, reference.get());
	}
	
}
