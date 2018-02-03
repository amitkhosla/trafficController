/**
 * 
 */
package org.ak.trafficController.messaging.mem;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * This class can be considered as extension of GenericInMemoryQueue.
 * This class is suitable for majority of use cases.
 * @author Amit Khosla
 *
 */
public class InMemoryQueue<T> {
	static Logger logger = Logger.getLogger(InMemoryQueue.class.getName());

	GenericInMemoryQueue<T> inMemoryQueue;

	private Consumer<T> directConsumer;

	private Consumer<List<T>> batchConsumer;

	private AtomicInteger directConsumerResetIndex = new AtomicInteger(0);
	private AtomicInteger batchConsumerResetIndex = new AtomicInteger(0);
	
	private int directConsumerCount = 1;

	private int batchConsumerCount = 1;

	
	Consumer<Integer> unregisterDirectConsumer = i->inMemoryQueue.unregister(getDirectConsumerName(i));
	Consumer<Integer> registerDirectConsumer = i-> inMemoryQueue.register(directConsumer, getDirectConsumerName(i));
	Consumer<Integer> unregisterBatchConsumer = i->inMemoryQueue.unregister(getBatchConsumerName(i));
	Consumer<Integer> registerBatchConsumer = i-> inMemoryQueue.registerBatchConsumer(batchConsumer, getBatchConsumerName(i));


	protected String getBatchConsumerName(Integer i) {
		return "batchConsumer" + i + "_" + batchConsumerResetIndex.get();
	}
	


	protected String getDirectConsumerName(Integer i) {
		return "directConsumer" + i + "_" + directConsumerResetIndex.get();
	}
	
	/**
	 * Creates a new in memory queue.
	 * @param queueName Name of queue.
	 */
	public InMemoryQueue(String queueName) {
		inMemoryQueue = new GenericInMemoryQueue<>(queueName);
	}
	
	/**
	 * Set batch size of the queue.
	 * @param batchSize Batch size
	 * @return InMemoryQueue for ease of use
	 */
	public InMemoryQueue<T> setBatchSize(int batchSize) {
		this.inMemoryQueue.setBatchSize(batchSize);
		return this;
	
	}

	protected void processForCounter(int start, int end, Consumer<Integer> consumer) {
		for (int i=start; i<=end;i++) {
			consumer.accept(i);
		}
	}
	
	public InMemoryQueue<T> setDirectConsumer(Consumer<T> consumer) {
		this.directConsumer =consumer;
		if (this.directConsumerCount > 0) {
			resetConsumer(unregisterDirectConsumer, registerDirectConsumer, this.directConsumerCount, directConsumerResetIndex);
		}
		return this;
	}

	protected void resetConsumer(Consumer<Integer> unregisterConsumer, Consumer<Integer> registerConsumer, int consumerCount, AtomicInteger index) {
		processForCounter(0, consumerCount, unregisterConsumer);
		index.incrementAndGet();
		processForCounter(1, consumerCount,registerConsumer);
	}

	public InMemoryQueue<T> setBatchConsumer(Consumer<List<T>> batchConsumer) {
		this.batchConsumer = batchConsumer;
		if (this.batchConsumerCount > 0) {
			resetConsumer(unregisterBatchConsumer, registerBatchConsumer, this.batchConsumerCount, this.batchConsumerResetIndex);
		}
		return this;
	}
	
	
	protected void setConsumerCount(int currentCount, int newCount, Consumer<Integer> registerCounsumer, Consumer<Integer> unregisterConsumer) {
		if (currentCount<newCount) {
			processForCounter(currentCount+1, newCount, registerCounsumer);
		} else if (currentCount>newCount) {
			processForCounter(newCount+1, currentCount, unregisterConsumer);
		}
	}
	
	public InMemoryQueue<T> setDirectConsumerCount(int directConsumerCount) {
		setConsumerCount(this.directConsumerCount, directConsumerCount, registerDirectConsumer, unregisterDirectConsumer);
		this.directConsumerCount = directConsumerCount;
		return this;
	}

	
	/**
	 * Reset batch consumer count. 
	 * @param batchConsumerCount batch consumers count
	 * @return Same instance on which we are looking for setting consumers
	 */
	public InMemoryQueue<T> setBatchConsumerCount(int batchConsumerCount) {
		setConsumerCount(this.batchConsumerCount, batchConsumerCount, registerBatchConsumer, unregisterBatchConsumer);
		this.batchConsumerCount = batchConsumerCount;
		return this;
	}

	/**
	 * Shutdown the queue.
	 * All consumers are shutdown. Should be called before last use of queue.
	 */
	public void shutdown() {
		this.inMemoryQueue.shutdown();
	}

	public void addAllFromList(Collection<T> input) {
		this.inMemoryQueue.add(input);
	}

	public void add(T i) {
		this.inMemoryQueue.add(i);
	}

	public Integer getBatchConsumerCount() {
		return this.batchConsumerCount;
	}
	
	public Integer getDirectConsumerCount() {
		return this.directConsumerCount;
	}

	public Long getNumberOfItemsInQueue() {
		return this.inMemoryQueue.getNumberOfItemsInQueue();
	}

	/**
	 * Increment direct consumer count.
	 */
	public void incrementDirectConsumer() {
		this.directConsumerCount++;
		this.inMemoryQueue.register(this.directConsumer, getDirectConsumerName(this.directConsumerCount));
	}
	
	/**
	 * Decrement direct consumer count.
	 */
	public void decrementDirectConsumer() {
		this.inMemoryQueue.unregister(getDirectConsumerName(this.directConsumerCount));
		this.directConsumerCount--;
	}
	
	/**
	 * Increment batch consumer count.
	 */
	public void incrementBatchConsumer() {
		this.batchConsumerCount++;
		this.inMemoryQueue.registerBatchConsumer(this.batchConsumer, getBatchConsumerName(this.batchConsumerCount));
	}
	
	/**
	 * Decrement batch consumer count.
	 */
	public void decrementBatchConsumer() {
		this.inMemoryQueue.unregister(getBatchConsumerName(this.batchConsumerCount));
		this.batchConsumerCount--;
	}
	
	public boolean isDirectConsumerSet() {
		return this.directConsumer != null;
	}
	
	public boolean isBatchConsumerSet() {
		return this.batchConsumer != null;
	}

	protected void clear(Consumer<Queue<T>>... consumer) {
		this.inMemoryQueue.clear(consumer);
	}



	/**
	 * Remove direct consumer
	 */
	public void removeDirectConsumer() {
		this.directConsumer = null;
		inMemoryQueue.removeAllDirectConsumers();
	}
	
	public void removeBatchConsumer() {
		this.batchConsumer = null;
		inMemoryQueue.removeAllBatchConsumers();
	}
}
