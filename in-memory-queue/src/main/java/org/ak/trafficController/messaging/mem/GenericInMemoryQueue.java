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
	
	private boolean hasShutdown;
	
	/**
	 * The value contains the channel name.
	 */
	private String queueName;

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
	
	private Collection<String> batchConsumersList = new ConcurrentLinkedQueue<>();

	private AtomicLong numberOfItemsInQueue = new AtomicLong(0L);
	
	public GenericInMemoryQueue(String name) {
		this.queueName = name;
	}
	
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
			} catch (Exception e) {
				logError(getExceptionMessage(consumerName, item) + e, e);
			}
		}
	}

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

	public StringBuilder buildErrorMessage(String consumerName) {
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
							dataQueue.wait();
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
			} catch (Exception e) {
				logError(this.getExceptionMessage(consumerName, output), e);
			}

		}
	}

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

	protected void doNotify() {
		synchronized (dataQueue) {
			dataQueue.notifyAll();
		}
	}
	
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

	public int getDirectConsumerCount() {
		return directConsumersList.size();
	}

	public int getBatchConsumerCount() {
		return batchConsumersList.size();
	}

	/**
	 * @return the hasShutdown
	 */
	public boolean isHasShutdown() {
		return hasShutdown;
	}

	/**
	 * @param hasShutdown the hasShutdown to set
	 */
	public void setHasShutdown(boolean hasShutdown) {
		this.hasShutdown = hasShutdown;
	}

	/**
	 * @return the queueName
	 */
	public String getQueueName() {
		return queueName;
	}

	/**
	 * @param queueName the queueName to set
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	/**
	 * @return the batchSize
	 */
	public int getBatchSize() {
		return batchSize;
	}

	/**
	 * @param batchSize the batchSize to set
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * @return the numberOfItemsInQueue
	 */
	public Long getNumberOfItemsInQueue() {
		return numberOfItemsInQueue.get();
	}

	/**
	 * @param numberOfItemsInQueue the numberOfItemsInQueue to set
	 */
	public void setNumberOfItemsInQueue(AtomicLong numberOfItemsInQueue) {
		this.numberOfItemsInQueue = numberOfItemsInQueue;
	}

	protected void clear(Consumer<Queue<T>>... preCleanHandlers) {
		if (preCleanHandlers != null) {
			for (Consumer<Queue<T>> c : preCleanHandlers) {
				c.accept(dataQueue);
			}
		}
		this.numberOfItemsInQueue.set(0);
		this.dataQueue.clear();
	}
	
	public void removeAllDirectConsumers() {
		this.directConsumersList.clear();
		doNotify();
	}
	
	public void removeAllBatchConsumers() {
		this.batchConsumersList.clear();
		doNotify();
	}

}
