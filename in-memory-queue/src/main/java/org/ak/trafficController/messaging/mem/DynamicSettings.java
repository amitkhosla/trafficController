package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.messaging.exception.ThresholdReachedException;

/**
 * This class handles dynamic settings of flow of messages.
 * This class helps {@code InMemoryQueueTuner} to tune as per configuration set by user.
 * @author Amit Khosla
 *
 * @param <T>
 */
public class DynamicSettings<T> { //We can later think of making it decorator
	
	static Logger log = Logger.getLogger(DynamicSettings.class.getName());
	
	private List<Consumer<Queue<T>>> cleaners = new ArrayList<>(); 
	private InMemoryQueue<T> queue;
	public InMemoryQueue<T> getQueue() {
		return queue;
	}

	public DynamicSettings<T> setQueue(InMemoryQueue<T> queue) {
		this.queue = queue;
		return this;
	}

	public Boolean getShouldThrowExceptionPostRetry() {
		return shouldThrowExceptionPostRetry;
	}

	public DynamicSettings<T> setShouldThrowExceptionPostRetry(Boolean shouldThrowExceptionPostRetry) {
		this.shouldThrowExceptionPostRetry = shouldThrowExceptionPostRetry;
		return this;
	}
	private Integer maxDirectConsumer=10;
	private Integer minDirectConsumer=1;

	private Integer maxBatchConsumer=10;
	private Integer minBatchConsumer=1;

	private Long highLimitWhenToIncreaseConsumer=500L;
	private Long lowLimitWhenToDecreseConsumer=50L;
	private Long thresholdWhenNoMoreItemsShouldBeHandled=5000L;
	
	private boolean shouldClearQueueAtThreshold;
	private boolean shouldStopAddingAtThreshold;
	private boolean shouldThrowExceptionWhenThresholdAtAdd;
	
	private boolean shouldPauseSenderTillThresholdNotRecovered;
	private Long waitForRetryThresholdLimit=100L;//milliseconds to wait for
	private Long numberOfRetriesToWait=100L; 
	private boolean shouldThrowExceptionPostRetry;
	
	
	public static void sleep(Long sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			log.log(Level.FINE, e, ()->"Could not sleep");
		}
	}

	public Integer getMaxDirectConsumer() {
		return maxDirectConsumer;
	}
	public DynamicSettings<T> setMaxDirectConsumer(Integer maxDirectConsumer) {
		this.maxDirectConsumer = maxDirectConsumer;
		return this;
	}
	public Integer getMinDirectConsumer() {
		return minDirectConsumer;
	}
	public DynamicSettings<T> setMinDirectConsumer(Integer minDirectConsumer) {
		this.minDirectConsumer = minDirectConsumer;
		return this;
	}
	public Integer getMaxBatchConsumer() {
		return maxBatchConsumer;
	}
	public DynamicSettings<T> setMaxBatchConsumer(Integer maxBatchConsumer) {
		this.maxBatchConsumer = maxBatchConsumer;
		return this;
	}
	public Integer getMinBatchConsumer() {
		return minBatchConsumer;
	}
	public DynamicSettings<T> setMinBatchConsumer(Integer minBatchConsumer) {
		this.minBatchConsumer = minBatchConsumer;
		return this;
	}
	public Long getHighLimitWhenToIncreaseConsumer() {
		return highLimitWhenToIncreaseConsumer;
	}
	public DynamicSettings<T> setHighLimitWhenToIncreaseConsumer(Long highLimitWhenToIncreaseConsumer) {
		this.highLimitWhenToIncreaseConsumer = highLimitWhenToIncreaseConsumer;
		return this;
	}
	public Long getLowLimitWhenToDecreseConsumer() {
		return lowLimitWhenToDecreseConsumer;
	}
	public DynamicSettings<T> setLowLimitWhenToDecreseConsumer(Long lowLimitWhenToDecreseConsumer) {
		this.lowLimitWhenToDecreseConsumer = lowLimitWhenToDecreseConsumer;
		return this;
	}
	public Long getThresholdWhenNoMoreItemsShouldBeHandled() {
		return thresholdWhenNoMoreItemsShouldBeHandled;
	}
	public DynamicSettings<T> setThresholdWhenNoMoreItemsShouldBeHandled(Long thresholdWheNoMoreItemsShouldBeHandled) {
		this.thresholdWhenNoMoreItemsShouldBeHandled = thresholdWheNoMoreItemsShouldBeHandled;
		return this;
	}
	public Boolean getShouldClearQueueAtThreshold() {
		return shouldClearQueueAtThreshold;
	}
	public DynamicSettings<T> setShouldClearQueueAtThreshold(Boolean shouldClearQueueAtThreshold) {
		this.shouldClearQueueAtThreshold = shouldClearQueueAtThreshold;
		return this;
	}
	public Boolean getShouldStopAddingAtThreshold() {
		return shouldStopAddingAtThreshold;
	}
	public DynamicSettings<T> setShouldStopAddingAtThreshold(Boolean shouldStopAddingAtThreshold) {
		this.shouldStopAddingAtThreshold = shouldStopAddingAtThreshold;
		return this;
	}
	public Boolean getShouldPauseSenderTillThresholdNotRecovered() {
		return shouldPauseSenderTillThresholdNotRecovered;
	}
	public DynamicSettings<T> setShouldPauseSenderTillThresholdNotRecovered(Boolean shouldPauseSenderTillThresholdNotRecovered) {
		this.shouldPauseSenderTillThresholdNotRecovered = shouldPauseSenderTillThresholdNotRecovered;
		return this;
	}
	public Long getWaitForRetryThresholdLimit() {
		return waitForRetryThresholdLimit;
	}
	public DynamicSettings<T> setWaitForRetryThresholdLimit(Long waitForRetryThresholdLimit) {
		this.waitForRetryThresholdLimit = waitForRetryThresholdLimit;
		return this;
	}
	public Long getNumberOfRetriesToWait() {
		return numberOfRetriesToWait;
	}
	public DynamicSettings<T> setNumberOfRetriesToWait(Long numberOfRetriesToWait) {
		this.numberOfRetriesToWait = numberOfRetriesToWait;
		return this;
	}

	public void adjust() {
		Long numberOfItemsInQueue = queue.getNumberOfItemsInQueue();
		if (numberOfItemsInQueue > highLimitWhenToIncreaseConsumer) {
			increaseConsumerIfPossible();
			tryIfCanFreeUp();
		} else if (numberOfItemsInQueue < lowLimitWhenToDecreseConsumer) {
			decreaseConsumerIfPossible();
		} 
	}

	protected void tryIfCanFreeUp() {
		if (shouldClearQueueAtThreshold && queue.getNumberOfItemsInQueue() > thresholdWhenNoMoreItemsShouldBeHandled) {
			this.queue.clear(getCleaners());
		}
	}

	/**
	 * Get registered cleaners.
	 * @return Attached cleaners
	 */
	protected Consumer<Queue<T>>[] getCleaners() {
		if (cleaners.size() > 0) {
			Consumer<Queue<T>>[] arr = new Consumer[cleaners.size()];
			for (int i=0;i<cleaners.size();i++) {
				arr[i] = cleaners.get(i);
			}
			return arr;//(Consumer<Queue<T>>[]) cleaners.toArray();
		}
		return null;
	}

	protected void increaseConsumerIfPossible() {
		if (queue.isDirectConsumerSet()) {
			int directConsumers = queue.getDirectConsumerCount();
			if (directConsumers < maxDirectConsumer) {
				queue.incrementDirectConsumer();
			}
		}
		if (queue.isBatchConsumerSet()) {
			int batchConsumers = queue.getBatchConsumerCount();
			if (batchConsumers < maxBatchConsumer) {
				queue.incrementBatchConsumer();
			}
		}
	}
	
	protected void decreaseConsumerIfPossible() {
		if (queue.isDirectConsumerSet()) {
			int directConsumers = queue.getDirectConsumerCount();
			if (directConsumers > minDirectConsumer) {
				queue.decrementDirectConsumer();
			}
		}
		if (queue.isBatchConsumerSet()) {
			int batchConsumers = queue.getBatchConsumerCount();
			if (batchConsumers > minBatchConsumer) {
				queue.decrementBatchConsumer();
			}
		}
	}

	public Boolean getShouldThrowExceptionWhenThresholdAtAdd() {
		return shouldThrowExceptionWhenThresholdAtAdd;
	}

	public void setShouldThrowExceptionWhenThresholdAtAdd(Boolean shouldThrowExceptionWhenThresholdAtAdd) {
		this.shouldThrowExceptionWhenThresholdAtAdd = shouldThrowExceptionWhenThresholdAtAdd;
	}
	
	public boolean addItemInQueue(T item) {
		return processAddItem(this.queue::add, item);
	}

	protected <K> boolean processAddItem(Consumer<K> consumer, K item) {
		if(addStillAllowed()){
			consumer.accept(item);
			return true;
		}
		return false;
	}
	
	public boolean addItemsInQueue(List<T> list) {
		return processAddItem(this.queue::addAllFromList, list);
	}

	protected boolean addStillAllowed() {
		if (queue.getNumberOfItemsInQueue() >= this.thresholdWhenNoMoreItemsShouldBeHandled) {
			log.fine("Threshold reached.");
			if (this.shouldThrowExceptionWhenThresholdAtAdd) {
				throw new ThresholdReachedException();
			} else if (this.shouldStopAddingAtThreshold) {
				return handleStopAddingAtThreshold();
			}
		}
		return true;
	}

	/**
	 * This method is to handle in situation where we need to continue even when threshold reached.
	 * To continue, we can try again for given retries or directly return false.
	 * In case of waiting is allowed, this method will throw {@code ThresholdReachedException} in case all retries are over and still threshold is there.
	 * @return true if post retries items in queue lowers to less than threshold 
	 */
	protected boolean handleStopAddingAtThreshold() {
		if (!shouldPauseSenderTillThresholdNotRecovered) {
			log.fine("wait is not applicable. Returning false");
			return false;
		}
		if (waitForRetriesIfAny()) {
			log.fine("wait for retries succeed. Returning true");
			return true;
		}
		if (shouldThrowExceptionPostRetry) {
			log.fine("wait for retries failed. Throwing exception as configured.");
			throw new ThresholdReachedException("Threshold Reached and after waits also, cannot recover.");
		}
		log.fine("all retries done. Now no waiting required. Sending back with false.");
		return false;
	}

	protected boolean waitForRetriesIfAny() {
		for (long i=0;i<numberOfRetriesToWait;i++) {
			log.fine("Waiting for Threshold limit get cooled off..." + (i+1));
			sleep(this.waitForRetryThresholdLimit);
			if (queue.getNumberOfItemsInQueue() < this.thresholdWhenNoMoreItemsShouldBeHandled) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Add pre clean handler. This will be used if threshold of queued items is reached and is configured to clean queue post threshold.
	 * Before cleanup, the consumer attached will read all data queued.
	 * Removal from any consumer will mean removal from queue itself.
	 * One can add multiple pre clean handlers. They will be called one by one.
	 * @param preCleanHandler Hanlder to be called before clear
	 * @return Current object to make it more usable
	 */
	public DynamicSettings<T> addPreCleanHandler(Consumer<Queue<T>> preCleanHandler) {
		this.cleaners.add(preCleanHandler);
		return this;
	}
}
