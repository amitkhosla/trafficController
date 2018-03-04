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
 * Using this class means we are saying only one consumer (with multiple instance) is consuming (each for direct and batch).
 * {@link GenericInMemoryQueue} can have different type of consumers working on same data which means as queue logic, 
 * some data will move to one consumer and other data will move to different consumer. 
 * This use case is very rare, thus this new class tries to hnadle majority of normal use case.
 * As we have only one consumer of each type, we can think of adding or decreasing number of consumers easily.
 * @author Amit Khosla
 */
public class InMemoryQueue<T> {
	static Logger logger = Logger.getLogger(InMemoryQueue.class.getName());

	/**
	 * The actual queue.
	 */
	GenericInMemoryQueue<T> inMemoryQueue;

	/**
	 * Direct consumer logic.
	 */
	private Consumer<T> directConsumer;

	/**
	 * Batch consumer logic.
	 */
	private Consumer<List<T>> batchConsumer;

	/**
	 * Each time when we need to reset the consumers, the value is reset to ensure we have new consumers registered.
	 */
	private AtomicInteger directConsumerResetIndex = new AtomicInteger(0);
	/**
	 * Each time when we need to reset the consumers, the value is reset to ensure we have new consumers registered.
	 */
	private AtomicInteger batchConsumerResetIndex = new AtomicInteger(0);
	
	/**
	 * Count of direct consumers.
	 */
	private int directConsumerCount = 1;

	/**
	 * Count of batch consumers.
	 */
	private int batchConsumerCount = 1;

	//Consumers for regisering or unregister a consumer of queue.
	private Consumer<Integer> unregisterDirectConsumer = i->inMemoryQueue.unregister(getDirectConsumerName(i));
	private Consumer<Integer> registerDirectConsumer = i-> inMemoryQueue.register(directConsumer, getDirectConsumerName(i));
	private Consumer<Integer> unregisterBatchConsumer = i->inMemoryQueue.unregister(getBatchConsumerName(i));
	private Consumer<Integer> registerBatchConsumer = i-> inMemoryQueue.registerBatchConsumer(batchConsumer, getBatchConsumerName(i));


	/**
	 * Name of batch consumer.
	 * @param i Batch number id
	 * @return Name of batch consumer
	 */
	protected String getBatchConsumerName(Integer i) {
		return "batchConsumer" + i + "_" + batchConsumerResetIndex.get();
	}
	
	/**
	 * Name of direct consumer.
	 * @param i Direct consumer id
	 * @return Name of direct consumer
	 */
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

	/**
	 * Process for add/remove a consumer from given start to given end index.
	 * @param start Start index from which we have to perform operation
	 * @param end End index till which we have to perform operation
	 * @param consumer Consumer which will perform actual operation
	 */
	protected void processForCounter(int start, int end, Consumer<Integer> consumer) {
		for (int i=start; i<=end;i++) {
			consumer.accept(i);
		}
	}
	
	/**
	 * Set direct consumer which will work on the data present in queue.
	 * @param consumer Consumer logic
	 * @return This object for further modifications
	 */
	public InMemoryQueue<T> setDirectConsumer(Consumer<T> consumer) {
		this.directConsumer =consumer;
		if (this.directConsumerCount > 0) {
			resetConsumer(unregisterDirectConsumer, registerDirectConsumer, this.directConsumerCount, directConsumerResetIndex);
		}
		return this;
	}

	/**
	 * Whenever a consumer needs to be changed, this method is called to remove current consumers of queue and then add fresh consumers.
	 * @param unregisterConsumer Unregister consumer logic
	 * @param registerConsumer Register consumer logic
	 * @param consumerCount Count of consumers to maintain
	 * @param index Current index
	 */
	protected void resetConsumer(Consumer<Integer> unregisterConsumer, Consumer<Integer> registerConsumer, int consumerCount, AtomicInteger index) {
		processForCounter(0, consumerCount, unregisterConsumer);
		index.incrementAndGet();
		processForCounter(1, consumerCount,registerConsumer);
	}

	/**
	 * Set batch consumer logic
	 * @param batchConsumer Batch consumer
	 * @return This object for further modification
	 */
	public InMemoryQueue<T> setBatchConsumer(Consumer<List<T>> batchConsumer) {
		this.batchConsumer = batchConsumer;
		if (this.batchConsumerCount > 0) {
			resetConsumer(unregisterBatchConsumer, registerBatchConsumer, this.batchConsumerCount, this.batchConsumerResetIndex);
		}
		return this;
	}
	
	
	/**
	 * This method is responsible for increasing/decreasing consumer count.
	 * @param currentCount Current count
	 * @param newCount New count which needs to be set
	 * @param registerCounsumer Register logic
	 * @param unregisterConsumer Unregister logic
	 */
	protected void setConsumerCount(int currentCount, int newCount, Consumer<Integer> registerCounsumer, Consumer<Integer> unregisterConsumer) {
		if (currentCount<newCount) {
			processForCounter(currentCount+1, newCount, registerCounsumer);
		} else if (currentCount>newCount) {
			processForCounter(newCount+1, currentCount, unregisterConsumer);
		}
	}
	
	/**
	 * Set direct consumer count.
	 * @param directConsumerCount Direct consumer count
	 * @return This object for further modification
	 */
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

	/**
	 * This method adds data present in input collection
	 * @param input Collection containing data
	 */
	public void addAllFromCollection(Collection<T> input) {
		this.inMemoryQueue.add(input);
	}

	/**
	 * Add item to the queue.
	 * @param i Item to be added to queue
	 */
	public void add(T i) {
		this.inMemoryQueue.add(i);
	}

	/**
	 * Get current batch consumer count
	 * @return Batch consumer count
	 */
	public Integer getBatchConsumerCount() {
		return this.batchConsumerCount;
	}
	
	/**
	 * Get direct consumer count.
	 * @return Direct consumer count
	 */
	public Integer getDirectConsumerCount() {
		return this.directConsumerCount;
	}

	/**
	 * Get current number of items in queue.
	 * @return items in queue
	 */
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
	
	/**
	 * This method lets us know that for this queue, any direct consumer set.
	 * @return true if already some direct consumer set
	 */
	public boolean isDirectConsumerSet() {
		return this.directConsumer != null;
	}
	
	/**
	 * This method lets us know that for this queue, any batch consumer set.
	 * @return true if already some batch consumer set
	 */
	public boolean isBatchConsumerSet() {
		return this.batchConsumer != null;
	}

	/**
	 * This method clears the queue. All data present in queue is removed.
	 * The data present in queue is first consumed by passed consumers.
	 * @param consumer Consumers consuming data present in queue
	 */
	protected void clear(Consumer<Queue<T>>... consumer) {
		this.inMemoryQueue.clear(consumer);
	}



	/**
	 * Remove direct consumer.
	 */
	public void removeDirectConsumer() {
		this.directConsumer = null;
		inMemoryQueue.removeAllDirectConsumers();
	}
	
	/**
	 * Remove Batch Consumer.
	 */
	public void removeBatchConsumer() {
		this.batchConsumer = null;
		inMemoryQueue.removeAllBatchConsumers();
	}
}
