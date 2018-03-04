package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.messaging.exception.ThresholdReachedException;

/**
 * This class handles dynamic settings of flow of messages.
 * This class helps {@code InMemoryQueueTuner} to tune as per configuration set by user.
 * As name suggest, this enables dynamic settings like increasing and decreasing consumer count on basis of number of messages.
 * Actual queue will be wrapped inside the settings.
 * <br>
 * Consider we have the queue and production is very fast. If we do not have more consumer, we will soon end up to a situation where we can have out of memory etc.
 * For this, this class can be configured to choose a high limit post which if messages in queue increases to, a new consumer is tried.
 * <br>
 * If post increasing consumer, the number of messages start reducing or when we had just a flare where we had lot of messages, there is a need to reduce the consumers
 * which is handled once low limit is encountered.
 * <br>
 * We can reduce or increase this count to an extent where we do not have any consumer or we end up having so many consumers which itself causing slowness.
 * This is also handled by minimum consumer count and maximum count.
 * 
 * Also, this class allows queue to throw exception or ignore taking more messages.
 * Also, post a threshold, it can be configured to get clean itself if the messages can be allowed to.
 * @author Amit Khosla
 *
 * @param <T> Type of queue.
 */
public class DynamicSettings<T> { //We can later think of making it decorator
	
	static Logger log = Logger.getLogger(DynamicSettings.class.getName());
	
	/**
	 * Cleaners that will be used in case max threshold is reached and configured to wipe out all data, these consumers will be passed the queue to let it get saved.
	 */
	private List<Consumer<Queue<T>>> cleaners = new ArrayList<>(); 
	private InMemoryQueue<T> queue;
	public InMemoryQueue<T> getQueue() {
		return queue;
	}

	/**
	 * Setting queue to be worked upon.
	 * @param queue Queue which needs to be worked upon
	 * @return this object to make it easy to work on
	 */
	public DynamicSettings<T> setQueue(InMemoryQueue<T> queue) {
		this.queue = queue;
		return this;
	}

	/**
	 * Should throw exception post retries.
	 * @return true if configured so
	 */
	public Boolean getShouldThrowExceptionPostRetry() {
		return shouldThrowExceptionPostRetry;
	}

	/**
	 * Set if exception should be thrown to producers if threshold is reached and configured amount of retries are also taken. 
	 * @param shouldThrowExceptionPostRetry Boolean value to set
	 * @return this object to make it easy to work on
	 */
	public DynamicSettings<T> setShouldThrowExceptionPostRetry(Boolean shouldThrowExceptionPostRetry) {
		this.shouldThrowExceptionPostRetry = shouldThrowExceptionPostRetry;
		return this;
	}
	/**
	 * Maximum direct consumers which can be created. This is maximum till what we can increase.
	 */
	private Integer maxDirectConsumer=10;
	/**
	 * Minimum Direct consumers to which we can reduce.
	 */
	private Integer minDirectConsumer=1;

	/**
	 * Maximum batch consumers which can be created.
	 */
	private Integer maxBatchConsumer=10;
	/**
	 * Minimum batch consumers which can be reduced to.
	 */
	private Integer minBatchConsumer=1;

	/**
	 * High limit when a new consumer addition will be tried.
	 */
	private Long highLimitWhenToIncreaseConsumer=500L;
	/**
	 * Low limit when a consumer can be shut down as the production is now slow or consumption is good enough.
	 */
	private Long lowLimitWhenToDecreseConsumer=50L;
	/**
	 * Threshold limit post which it is considered as dangerous situation.
	 */
	private Long thresholdWhenNoMoreItemsShouldBeHandled=5000L;
	
	/**
	 * If set, once the threshold is reached we can clear the queue.
	 */
	private boolean shouldClearQueueAtThreshold;
	/**
	 * Should stop adding if already threshold reached.
	 */
	private boolean shouldStopAddingAtThreshold;
	/**
	 * Should throw exception when threshold is reached at time of adding new item.
	 */
	private boolean shouldThrowExceptionWhenThresholdAtAdd;
	
	/**
	 * Should retry producer till threshold not recovered.
	 */
	private boolean shouldRetrySenderTillThresholdNotRecovered;
	/**
	 * Milliseconds to wait for retry.
	 */
	private Long waitForRetryThresholdLimit=100L;//milliseconds to wait for
	/**
	 * Number of retries while waiting.
	 */
	private Long numberOfRetriesToWait=100L; 
	/**
	 * Should throw exception post retry or just reject.
	 */
	private boolean shouldThrowExceptionPostRetry;
	
	
	/**
	 * Sleep for configured amount.
	 * @param sleepTime Sleep time for which we want to sleep
	 */
	public static void sleep(Long sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			log.log(Level.FINE, e, ()->"Could not sleep");
		}
	}

	/**
	 * Maximum direct consumers which can be created. A new consumer will be added automatically once high limit is reached. This is maximum till what we can increase.
	 * @return Maximum direct consumer
	 */
	public Integer getMaxDirectConsumer() {
		return maxDirectConsumer;
	}
	/**
	 * Set the number of maximum direct consumers which can be created.
	 * A new consumer will be added automatically once high limit is reached.
	 * This is maximum till what we can increase. 
	 * @param maxDirectConsumer Maximum direct consumer
	 * @return This object for further modifications easily
	 */
	public DynamicSettings<T> setMaxDirectConsumer(Integer maxDirectConsumer) {
		this.maxDirectConsumer = maxDirectConsumer;
		return this;
	}
	/**
	 * Minimum direct consumers which can be reduced to. 
	 * A consumer is stopped automatically once count is less than low limit.
	 * This is minimum till what we can reduce.
	 * @return Minimum direct consumer count
	 */
	public Integer getMinDirectConsumer() {
		return minDirectConsumer;
	}
	/**
	 * Setter of mimimum direct consumers till which consumers count be reduced to.
	 * A consumer is stopped automatically once count is less than low limit.
	 * This is minimum till what we can reduce.
	 * @param minDirectConsumer Minimum direct consumer count to be set
	 * @return This object for further modifications easily
	 */
	public DynamicSettings<T> setMinDirectConsumer(Integer minDirectConsumer) {
		this.minDirectConsumer = minDirectConsumer;
		return this;
	}
	/**
	 * Maximum batch consumers which can be created. A new consumer will be added automatically once high limit is reached. This is maximum till what we can increase.
	 * @return Maximum batch consumer count
	 */
	public Integer getMaxBatchConsumer() {
		return maxBatchConsumer;
	}
	/**
	 *  Set the number of maximum batch consumers which can be created.
	 * A new consumer will be added automatically once high limit is reached.
	 * This is maximum till what we can increase. 
	 * @param maxBatchConsumer Maximum batch consumer
	 * @return This object for further modifications easily
	 */
	public DynamicSettings<T> setMaxBatchConsumer(Integer maxBatchConsumer) {
		this.maxBatchConsumer = maxBatchConsumer;
		return this;
	}
	/**
	 * Minimum batch consumers which can be reduced to. 
	 * A consumer is stopped automatically once count is less than low limit.
	 * This is minimum till what we can reduce.
	 * @return Minimum batch consumer count
	 */
	public Integer getMinBatchConsumer() {
		return minBatchConsumer;
	}
	/**
	 * Setter of minimum batch consumers till which consumers count be reduced to.
	 * A consumer is stopped automatically once count is less than low limit.
	 * This is minimum till what we can reduce.
	 * @param minBatchConsumer Minimum direct consumer count to be set
	 * @return This object for further modifications easily
	 */
	public DynamicSettings<T> setMinBatchConsumer(Integer minBatchConsumer) {
		this.minBatchConsumer = minBatchConsumer;
		return this;
	}
	/**
	 * Get high limit when increase of consumer is tried. If the number of messages in queue is greater than this value a new consumer will be tried.
	 * @return High limit when to increase consumer
	 */
	public Long getHighLimitWhenToIncreaseConsumer() {
		return highLimitWhenToIncreaseConsumer;
	}
	/**
	 * Set high limit when increase of consumer is tried. If the number of messages in queue is greater than this value a new consumer will be tried.
	 * @param highLimitWhenToIncreaseConsumer High limit when increase of consumer should be tried
	 * @return This object to easily configure urther
	 */
	public DynamicSettings<T> setHighLimitWhenToIncreaseConsumer(Long highLimitWhenToIncreaseConsumer) {
		this.highLimitWhenToIncreaseConsumer = highLimitWhenToIncreaseConsumer;
		return this;
	}
	/**
	 * Get low limit when decrease of consumer is tried. If number of messages in queue is lower than this value, a consumer can be tried to shut down.
	 * @return Low limit 
	 */
	public Long getLowLimitWhenToDecreseConsumer() {
		return lowLimitWhenToDecreseConsumer;
	}
	/**
	 * Set low limit when decrease of consumer is tried. If number of messages in queue is lower than this value, a consumer can be tried to shut down.
	 * @param lowLimitWhenToDecreseConsumer Low limit which should be taken in consideration
	 * @return This object to easily configure further
	 */
	public DynamicSettings<T> setLowLimitWhenToDecreseConsumer(Long lowLimitWhenToDecreseConsumer) {
		this.lowLimitWhenToDecreseConsumer = lowLimitWhenToDecreseConsumer;
		return this;
	}
	/**
	 * Threshold when more item should not be added.
	 * @return Threshold value
	 */
	public Long getThresholdWhenNoMoreItemsShouldBeHandled() {
		return thresholdWhenNoMoreItemsShouldBeHandled;
	}
	/**
	 * Threshold when more item should not be added.
	 * @param thresholdWheNoMoreItemsShouldBeHandled Threshold value to be configured to
	 * @return This object to further configure.
	 */
	public DynamicSettings<T> setThresholdWhenNoMoreItemsShouldBeHandled(Long thresholdWheNoMoreItemsShouldBeHandled) {
		this.thresholdWhenNoMoreItemsShouldBeHandled = thresholdWheNoMoreItemsShouldBeHandled;
		return this;
	}
	/**
	 * Should clear the queue if threshold reached.
	 * @return should clear flag
	 */
	public Boolean getShouldClearQueueAtThreshold() {
		return shouldClearQueueAtThreshold;
	}
	/**
	 * Configures to if should clear queue at threshold.
	 * @param shouldClearQueueAtThreshold boolean value
	 * @return This object for further configure
	 */
	public DynamicSettings<T> setShouldClearQueueAtThreshold(Boolean shouldClearQueueAtThreshold) {
		this.shouldClearQueueAtThreshold = shouldClearQueueAtThreshold;
		return this;
	}
	/**
	 * Should stop adding at threshold.
	 * @return should stop adding at threshold
	 */
	public Boolean getShouldStopAddingAtThreshold() {
		return shouldStopAddingAtThreshold;
	}
	/**
	 * Setter of should stop adding at threshold.
	 * @param shouldStopAddingAtThreshold value to be set
	 * @return This object to make it easy to further update this object.
	 */
	public DynamicSettings<T> setShouldStopAddingAtThreshold(Boolean shouldStopAddingAtThreshold) {
		this.shouldStopAddingAtThreshold = shouldStopAddingAtThreshold;
		return this;
	}
	/**
	 * Is retry enabled while adding if threshold is reached.
	 * @return Retry enabled status
	 */
	public Boolean getShouldRetrySenderTillThresholdNotRecovered() {
		return shouldRetrySenderTillThresholdNotRecovered;
	}
	/**
	 * Setter of retry enabled while adding if threshold is reached.
	 * @param shouldPauseSenderTillThresholdNotRecovered value to be set
	 * @return This object for easy further modification
	 */
	public DynamicSettings<T> setShouldRetrySenderTillThresholdNotRecovered(Boolean shouldPauseSenderTillThresholdNotRecovered) {
		this.shouldRetrySenderTillThresholdNotRecovered = shouldPauseSenderTillThresholdNotRecovered;
		return this;
	}
	/**
	 * Wait time for retries.
	 * @return wait time for retries
	 */
	public Long getWaitForRetryThresholdLimit() {
		return waitForRetryThresholdLimit;
	}
	/**
	 * Set wait time for retry.
	 * @param waitForRetryThresholdLimit Wait for retry value to be set
	 * @return This object for further modifications or use
	 */
	public DynamicSettings<T> setWaitForRetryThresholdLimit(Long waitForRetryThresholdLimit) {
		this.waitForRetryThresholdLimit = waitForRetryThresholdLimit;
		return this;
	}
	/**
	 * Number of retries while adding at threshold. 
	 * @return Number of retries
	 */
	public Long getNumberOfRetriesToWait() {
		return numberOfRetriesToWait;
	}
	/**
	 * Setter of Number of retries while adding at threshold.
	 * @param numberOfRetriesToWait Number of retries
	 * @return This object for further modifications or use
	 */
	public DynamicSettings<T> setNumberOfRetriesToWait(Long numberOfRetriesToWait) {
		this.numberOfRetriesToWait = numberOfRetriesToWait;
		return this;
	}

	/**
	 * This method handles the increase/decrease of consumers.
	 * A tuner calls this method periodically.
	 */
	public void adjust() {
		Long numberOfItemsInQueue = queue.getNumberOfItemsInQueue();
		if (numberOfItemsInQueue > highLimitWhenToIncreaseConsumer) {
			increaseConsumerIfPossible();
			tryIfCanFreeUp();
		} else if (numberOfItemsInQueue < lowLimitWhenToDecreseConsumer) {
			decreaseConsumerIfPossible();
		} 
	}

	/**
	 * It verifies if it can free up messages based on configurations and condition.
	 */
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

	/**
	 * Increase consumers if high limit is reached and we still have number of consumers less than total current consumers.
	 */
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
	
	/**
	 * Decrease consumer if low limit attained back and we still have more consumers than minimum consumers.
	 */
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

	/**
	 * Should throw exception when threshold reached in add process.
	 * @return Status of shouldThrowExceptionWhenThresholdAtAdd
	 */
	public Boolean getShouldThrowExceptionWhenThresholdAtAdd() {
		return shouldThrowExceptionWhenThresholdAtAdd;
	}

	/**
	 * Setter of Should throw exception when threshold reached in add process.
	 * @param shouldThrowExceptionWhenThresholdAtAdd Value to be set
	 * @return This object for further use
	 */
	public DynamicSettings<T> setShouldThrowExceptionWhenThresholdAtAdd(Boolean shouldThrowExceptionWhenThresholdAtAdd) {
		this.shouldThrowExceptionWhenThresholdAtAdd = shouldThrowExceptionWhenThresholdAtAdd;
		return this;
	}
	
	/**
	 * This method is responsible of adding data in queue.
	 * This method also retries and/or throws exception as per configuration.
	 * If addition is not allowed and no exception should be thrown, returns false.
	 * @param item Item to be added to queue
	 * @return True if successfully add item in queue
	 */
	public boolean addItemInQueue(T item) {
		return processAddItem(this.queue::add, item);
	}

	/**
	 * Checks if add item allowed and add the item to queue.
	 * @param consumer Consumer which will actually add item in queue
	 * @param item Item to be added
	 * @param <K> type of item to be added
	 * @return true if successfully added to queue 
	 */
	protected <K> boolean processAddItem(Consumer<K> consumer, K item) {
		if(addStillAllowed()){
			consumer.accept(item);
			return true;
		}
		return false;
	}
	
	/**
	 *  This method is responsible of adding data in list in queue.
	 * This method also retries and/or throws exception as per configuration.
	 * If addition is not allowed and no exception should be thrown, returns false.
	 * @param list List containing data to be saved in queue
	 * @return True if successfully added to queue
	 */
	public boolean addItemsInQueue(Collection<T> list) {
		return processAddItem(this.queue::addAllFromCollection, list);
	}

	/**
	 * This method tells if we can still add in the queue.
	 * @return true if allowed
	 */
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
		if (!shouldRetrySenderTillThresholdNotRecovered) {
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

	/**
	 * This method waits for configured number and for given time if allowed to wait.
	 * Post each retry, it checks if we threshold is now cooled off.
	 * @return true if we can add more data
	 */
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
