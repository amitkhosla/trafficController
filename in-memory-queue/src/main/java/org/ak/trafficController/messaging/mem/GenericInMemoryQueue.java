package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Each object of this class act as in memory queue. One can attach listener to it.
 * The listener can be of direct retrieval or list retrieval. 
 * One can consider them as event handler as well. 
 * Whenever any data added to it will be processed by available listener.
 * 
 *  This works on observer pattern where all listeners are waiting for notification which will be sent when any item is added.
 * 
 * @author Amit Khosla
 * @param <T> This defines the type of object the queue will work on.
 */
public class GenericInMemoryQueue<T> {

	static Logger logger = Logger.getLogger(GenericInMemoryQueue.class.getName());
	
	/**
	 * Shutdown flag, when all consumers will stop working.
	 */
	private boolean hasShutdown;
	
	/**
	 * The value contains the channel name.
	 */
	private String queueName;

	/**
	 * This is the time for which consumer wait for notification before retrying. This is for removing any missed signal.
	 */
	int waitTimeout = 1000;

	/**
	 * The value contains the batch size for batch consumers.
	 */
	private int batchSize = 10;

	/**
	 * This is the data queue where data will be kept before any listener can read.
	 */
	private Queue<T> dataQueue = new ConcurrentLinkedQueue<T>();

	/**
	 * The instance of {@link ConcurrentLinkedQueue} containing the direct consumer list.
	 */
	private Collection<String> directConsumersList = new ConcurrentLinkedQueue<>();
	
	/**
	 * The instance of {@link ConcurrentLinkedQueue} containing the batch consumer list.
	 */
	private Collection<String> batchConsumersList = new ConcurrentLinkedQueue<>();

	/**
	 * Current items in queue.
	 */
	private AtomicLong numberOfItemsInQueue = new AtomicLong(0L);
	
	private Long sleepPostConsumingAll = 0l;
	
	private boolean shouldSleepConsumingAll = false;
	
	private boolean shouldSleepAfterEveryConsumption = false;
	
	private Long sleepPostConsumingEvery = 0L;
	
	/**
	 * @return the sleepPostConsumingEvery
	 */
	public Long getSleepPostConsumingEvery() {
		return sleepPostConsumingEvery;
	}

	/**
	 * This method sets sleepPostConsumingEvery with the value passed.
	 * @param sleepPostConsumingEvery the sleepPostConsumingEvery to set
	 * @return Self to use further
	 */
	public GenericInMemoryQueue<T> setSleepPostConsumingEach(Long sleepPostConsumingEvery) {
		this.sleepPostConsumingEvery = sleepPostConsumingEvery;
		this.shouldSleepAfterEveryConsumption = true;
		return this;
	}

	/**
	 * @return the shouldSleepConsumingAll
	 */
	public boolean isShouldSleepConsumingAll() {
		return shouldSleepConsumingAll;
	}

	/**
	 * @return the shouldSleepAfterEveryConsumption
	 */
	public boolean isShouldSleepAfterEveryConsumption() {
		return shouldSleepAfterEveryConsumption;
	}

	/**
	 * @return the sleepPostConsumingAll
	 */
	public Long getSleepPostConsumingAll() {
		return sleepPostConsumingAll;
	}

	/**
	 * This method sets sleepPostConsumingAll with the value passed.
	 * @param sleepPostConsumingAll the sleepPostConsumingAll to set
	 * @return Self to use further
	 */
	public GenericInMemoryQueue<T> setSleepPostConsumingAll(Long sleepPostConsumingAll) {
		this.sleepPostConsumingAll = sleepPostConsumingAll;
		this.shouldSleepConsumingAll = true;
		return this;
	}

	/**
	 * Constructor of Generic in memory queue.
	 * @param name Name of queue
	 */
	public GenericInMemoryQueue(String name) {
		this.queueName = name;
	}
	
	/**
	 * Constructor which also configures batch size which is used by batch consumers.
	 * Batch size set here is the maximum items in list a consumer can expect.
	 * @param name Name of queue
	 * @param batchSize Batch size
	 */
	public GenericInMemoryQueue(String name, int batchSize) {
		this.queueName = name;
		this.batchSize = batchSize;
	}
	
	/**
	 * This method register the direct consumer.
	 * 
	 * @param consumer
	 *            The instance of {@link Consumer} to set
	 * @param consumerName
	 *            The consumer name.
	 */
	public void register(Consumer<T> consumer, String consumerName) {
		if (!directConsumersList.contains(consumerName)) {
			startListening(
					() -> singleItemProcess(consumer, consumerName),
					consumerName, 
					directConsumersList
			);
		}
	}

	
	
	/**
	 * This method polls from the queue and process the consumer with data found.
	 * The method keeps control till there is any data in queue.
	 * 
	 * @param consumer
	 *            The {@link Consumer} instance to set
	 * @param consumerName
	 *            The {@link String} consumer name to set
	 */
	protected void singleItemProcess(Consumer<T> consumer, String consumerName) {
		while (!dataQueue.isEmpty()) {
			T item = dataQueue.poll();
			if (item == null) {
				continue;
			}
			numberOfItemsInQueue.decrementAndGet();
			try {
				consumer.accept(item);
				sleepIfConfiguredPostEveryConsumption();
			} catch (Exception e) {
				logError(getExceptionMessage(consumerName, item) + e, e);
			}
		}
		sleepIfConfigured();
	}

	/**
	 * 
	 */
	protected void sleepIfConfiguredPostEveryConsumption() {
		if (shouldSleepAfterEveryConsumption) {
			try {
				Thread.sleep(sleepPostConsumingEvery);
			} catch (InterruptedException e) {
				logError("Exception occured post processing a message and while sleeping", e);
			}
		}
	}

	/**
	 * Sleep post consuming all messages if configured.
	 */
	protected void sleepIfConfigured() {
		if (shouldSleepConsumingAll) {
			try {
				Thread.sleep(sleepPostConsumingAll);
			} catch (InterruptedException e) {
				logError("Exception occured post processing all messages and while sleeping", e);
			}
		}
	}

	/**
	 * Log error if some exception occurs.
	 * @param message Error message
	 * @param e Exception to be logged
	 */
	private void logError(String message, Exception e) {
		logger.log(Level.WARNING, message, e);
	}

	/**
	 * This method prepares the exception message containing name of consumer and queue.
	 * 
	 * @param consumerName
	 *            The {@link String} consumer name
	 * @param item
	 *            The item of T type to set
	 * @return The {@link String} exception message
	 */
	protected String getExceptionMessage(String consumerName, T item) {
		StringBuilder sb = buildErrorMessage(consumerName)
		.append(" while consuming for item :").append(item);
		return sb.toString();
	}

	/**
	 * This method builds the exception message for batch consumer.
	 * 
	 * @param consumerName
	 *            the consumerName
	 * @param items
	 *            the list of items
	 * @return String append message
	 */
	protected String getExceptionMessage(String consumerName, List<T> items) {
		StringBuilder sb = buildErrorMessage(consumerName)
			.append(" while consuming for items :");
		items.forEach(item->sb.append(item).append(","));
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	/**
	 * Build error message using consumer name and queue name.
	 * @param consumerName Consumer name
	 * @return Message built in string builder
	 */
	protected StringBuilder buildErrorMessage(String consumerName) {
		StringBuilder sb = new StringBuilder();
		return sb.append("Exception occured in queue ").append(this.queueName)
			.append(" for consumer : ").append(consumerName);
	}

	/**
	 * Register batch consumer
	 * @param consumer
	 *            the consumer
	 * @param consumerName
	 *            the consumer name
	 */
	public void registerBatchConsumer(Consumer<List<T>> consumer, String consumerName) {
		if (!batchConsumersList.contains(consumerName)) {
			startListening(() -> batchProcess(consumer, consumerName), consumerName, batchConsumersList);
		}
	}

	/**
	 * This method start listener. It creates a thread which will perform the operation required on the message in queue.
	 * 
	 * @param consumerLogic
	 *            The instance of {@link Runnable} to set
	 * @param consumerName
	 *            The {@link String} consumer name to set
	 * @param consumerList consumersList direct or batch
	 */
	protected void startListening(Runnable consumerLogic, String consumerName, Collection<String> consumerList) {
		consumerList.add(consumerName);
		Runnable executionLogic = () -> {
			while (!hasShutdown && consumerList.contains(consumerName)) {
				consumerLogic.run();
				if (consumerList.contains(consumerName)) {
					try {
						synchronized (dataQueue) {
							dataQueue.wait(waitTimeout);
						}
					} catch (InterruptedException e) {
						logError("Interrupted exception occured in channel", e);
					}
				}
			}
			doNotify();
		};
		String name = this.queueName + "-" + consumerName;
		
		startConsumer(executionLogic, name);
	}

	/**
	 * This method starts the consumer. This method will decide whether to use any executor or create new thread.
	 * Currently it looks like creating thread is better than managing executors as these threads will keep on running.
	 * @param executionLogic Execution logic
	 * @param name Name of consumer which will be set as name.
	 */
	protected void startConsumer(Runnable executionLogic, String name) {
		Thread t= new Thread(executionLogic, name);
		t.start();
	}

	/**
	 * This method gets from the queue and process.
	 * 
	 * @param consumer
	 *            The {@link Consumer} instance to set
	 * @param consumerName
	 *            The {@link String} consumer name to set
	 */
	protected void batchProcess(Consumer<List<T>> consumer, String consumerName) {
		while (!dataQueue.isEmpty()) {
			List<T> output = getBatchData();
			if (output.isEmpty()) {
				continue;
			}
			try {
				consumer.accept(output);
				sleepIfConfiguredPostEveryConsumption();
			} catch (Exception e) {
				logError(this.getExceptionMessage(consumerName, output), e);
			}

		}
		sleepIfConfigured();
	}

	/**
	 * Get batch data which will be used to pass to batch consumer.
	 * This method tries to get maximum "batchsize" messages.
	 * It will not wait if the number of messages is lesser.
	 * @return List containing data
	 */
	protected List<T> getBatchData() {
		int itemsPresentInOutputList = 0;
		List<T> output = new ArrayList<>();
		while (!dataQueue.isEmpty() && itemsPresentInOutputList++ < batchSize) {
			T item = dataQueue.poll();
			if (item != null) {
				output.add(item);
				numberOfItemsInQueue.decrementAndGet();
			} else {
				break;
			}
		}
		
		return output;
	}

	/**
	 * This method unregister the consumer.
	 * 
	 * @param consumerName
	 *            The {@link String} consumer name to remove
	 */
	public void unregister(String consumerName) {
		directConsumersList.remove(consumerName);
		batchConsumersList.remove(consumerName);
		doNotify();
	}

	/**
	 * This method Notify all consumers.
	 * This is done whenever a new message arrives or a consumer is asked to stop.
	 */
	protected void doNotify() {
		synchronized (dataQueue) {
			dataQueue.notifyAll();
		}
	}
	
	/**
	 * To cleanup, shutdown method should be called.
	 * This method shutdown all consumers batch or direct.
	 */
	public void shutdown() {
		this.hasShutdown = true;
		directConsumersList.clear();
		batchConsumersList.clear();
		doNotify();
	}

	/**
	 * This method add an item to the queue.
	 * 
	 * @param item
	 *            The item to add
	 */
	public void add(T item) {
		dataQueue.add(item);
		numberOfItemsInQueue.incrementAndGet();
		notifyConsumers();
	}

	/**
	 * This method notify on addition of an item in the queue.
	 */
	protected void notifyConsumers() {
		if (!directConsumersList.isEmpty() || !batchConsumersList.isEmpty()) {
			doNotify();
		}
	}

	/**
	 * This method adds list of items to be processed in queue.
	 * 
	 * @param items
	 *            The {@link List} of items to add
	 */
	public void add(Collection<T> items) {
		dataQueue.addAll(items);
		numberOfItemsInQueue.addAndGet(items.size());
		notifyConsumers();
	}

	/**
	 * Get number of direct consumers.
	 * @return Direct consumers count
	 */
	public int getDirectConsumerCount() {
		return directConsumersList.size();
	}

	/**
	 * Get batch consumer count.
	 * @return Batch consumer count
	 */
	public int getBatchConsumerCount() {
		return batchConsumersList.size();
	}

	/**
	 * Have this queue shutdown?
	 * @return Have this queue shutdown?
	 */
	public boolean isHasShutdown() {
		return hasShutdown;
	}


	/**
	 * Get queue name.
	 * @return the queueName
	 */
	public String getQueueName() {
		return queueName;
	}

	/**
	 * Set name of the queue
	 * @param queueName the queueName to set
	 * @return This object for further use
	 */
	public GenericInMemoryQueue<T> setQueueName(String queueName) {
		this.queueName = queueName;
		return this;
	}

	/**
	 * Batch size which will be used by batch consumers.
	 * Any batch consumer will receive maximum of this many data items in list.
	 * The batch consumer might get fewer amount as well as consumers are called with 
	 * only current data in queue and not waited for this list to be filled
	 * @return the batchSize
	 */
	public int getBatchSize() {
		return batchSize;
	}

	/**
	 * Setter Batch size which will be used by batch consumers.
	 * Any batch consumer will receive maximum of this many data items in list.
	 * The batch consumer might get fewer amount as well as consumers are called with 
	 * only current data in queue and not waited for this list to be filled
	 * @param batchSize the batchSize to set
	 * @return This object for further use
	 */
	public GenericInMemoryQueue<T> setBatchSize(int batchSize) {
		this.batchSize = batchSize;
		return this;
	}

	/**
	 * Get number of items in queue. 
	 * This method is used for diagnostics or by some tuner which want to resize number of consumers.
	 * @return the numberOfItemsInQueue
	 */
	public Long getNumberOfItemsInQueue() {
		return numberOfItemsInQueue.get();
	}

	/**
	 * Set number of items in queue. 
	 * This method is used for diagnostics or by some tuner which want to resize number of consumers.
	 * @param numberOfItemsInQueue the numberOfItemsInQueue to set
	 * @return This object for further use
	 */
	public GenericInMemoryQueue<T> setNumberOfItemsInQueue(AtomicLong numberOfItemsInQueue) {
		this.numberOfItemsInQueue = numberOfItemsInQueue;
		return this;
	}

	/**
	 * This method clears the current queue and pass the data to the consumer.
	 * Post this data is consumed by cleaner, queue is cleaned.
	 * This method is used by scenarios where the messages are allowed to be wiped off to save the health of the system.
	 * This method is called by some tuner. 
	 * @param preCleanHandlers Cleaners
	 */
	protected void clear(Consumer<Queue<T>>... preCleanHandlers) {
		if (preCleanHandlers != null) {
			for (Consumer<Queue<T>> c : preCleanHandlers) {
				c.accept(dataQueue);
			}
		}
		this.numberOfItemsInQueue.set(0);
		this.dataQueue.clear();
	}
	
	/**
	 * Removes all direct consumers.
	 * This method is called either at shutdown or we want to move to different logic of processing the data.
	 */
	public void removeAllDirectConsumers() {
		this.directConsumersList.clear();
		doNotify();
	}
	
	/**
	 * Remove all batch consumers.
	 * This method is called either at shutdown or we want to move to different logic of processing the data.
	 */
	public void removeAllBatchConsumers() {
		this.batchConsumersList.clear();
		doNotify();
	}

	/**
	 * This is the wait time for which different consumers wait for some message. 
	 * Post wait, the consumers retry finding data and if not found go to wait again.
	 * In rare case if some notification is lost, this mechanism guarantees that messages will be picked if present post this time. 
	 * @return Wait time
	 */
	public int getWaitTimeout() {
		return waitTimeout;
	}

	/**
	 * This is the wait time for which different consumers wait for some message. 
	 * Post wait, the consumers retry finding data and if not found go to wait again.
	 * In rare case if some notification is lost, this mechanism guarantees that messages will be picked if present post this time.
	 * @param waitTimeout wait time out to be set
	 * @return This object to further use
	 */
	public GenericInMemoryQueue<T> setWaitTimeout(int waitTimeout) {
		this.waitTimeout = waitTimeout;
		return this;
	}

}
