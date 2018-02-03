package org.ak.trafficController.messaging.mem;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This queue manager creates multiple in memory queue.
 * This helps user to directly create queue, add listeners, add data without taking any reference.
 * @author Amit Khosla
 */
public class InMemoryQueueManager {

	protected Map<String, InMemoryQueue> queues = new HashMap<String, InMemoryQueue>();

	protected Map<String, DynamicSettings> dynamicSettings = new HashMap<>();
	
	public <T> void initialize(String queueName, Consumer<T> consumer, int numberOfConsumers) {
		InMemoryQueue<T> queue = registerQueueIfAbsent(queueName);
		setConsumers(consumer, numberOfConsumers, queue);
	}
	
	public <T> void initializeForBatch(String queueName, Consumer<List<T>> consumer, int numberOfConsumers) {
		InMemoryQueue<T> queue = registerQueueIfAbsent(queueName);
		setBatchConsumers(consumer, numberOfConsumers, queue);
	}

	/**
	 * This makes sure only one queue is created for a given name.
	 * @param queueName Name of the queue
	 * @return Queue for the given queue
	 */
	synchronized protected <T> InMemoryQueue<T> registerQueueIfAbsent(String queueName) {
		InMemoryQueue<T> queue = queues.get(queueName);
		if (queue == null) {
			queue =new InMemoryQueue<T>(queueName);
			queues.put(queueName, queue);
		}
		return queue;
	}

	/**
	 * Set consumers in a given queue.
	 * If already attached consumers is less than required, this method register more.
	 * Else, this method unregister consumers.
	 * @param consumer Consumer of the queue which is looking for working on data present in queue.
	 * @param numberOfConsumers number of consumers required for queue to 
	 * @param queue Queue on which we are looking for setting consumers.
	 */
	protected <T> void setConsumers(Consumer<T> consumer,
			int numberOfConsumers, InMemoryQueue<T> queue) {
		queue.setDirectConsumer(consumer);
		queue.setDirectConsumerCount(numberOfConsumers);
	}
	
	/**
	 * Set batch consumers in a given queue. Batch consumers work on batch of data instead of individual records.
	 * If already attached consumers is less than required, this method register more.
	 * Else, this method unregister consumers.
	 * @param consumer Consumer of the queue which is looking for working on data present in queue.
	 * @param numberOfConsumers number of consumers required for queue to 
	 * @param queue Queue on which we are looking for setting consumers.
	 */
	protected <T> void setBatchConsumers(Consumer<List<T>> consumer,
			int numberOfConsumers, InMemoryQueue<T> queue) {
		queue.setBatchConsumer(consumer);
		queue.setBatchConsumerCount(numberOfConsumers);
	}
	
	/**
	 * Add message to the queue.
	 * @param queueName
	 * @param item
	 * @return
	 */
	public <T> boolean add(String queueName, T item) {
		DynamicSettings<T> dynamicSetting = dynamicSettings.get(queueName);
		if (dynamicSetting != null) {
			return dynamicSetting.addItemInQueue(item);
		}
		InMemoryQueue<T> imq = queues.get(queueName);
		if (imq == null) {
			return false;
		}
		imq.add(item);
		return true;
	}
	
	/**
	 * Add messages to the queue.
	 * @param queueName
	 * @param items
	 * @return
	 */
	public <T> boolean addItems(String queueName, Collection<T> items) {
		DynamicSettings<T> dynamicSetting = dynamicSettings.get(queueName);
		if (dynamicSetting != null) {
			return dynamicSetting.addItemsInQueue(items);
		}
		InMemoryQueue<T> imq = queues.get(queueName);
		if (imq == null) {
			return false;
		}
		imq.addAllFromList(items);
		return true;
	}
	
	public <T> void addListener(String queueName, Consumer<T> consumer, int numberOfConsumers) {
		InMemoryQueue<T> queue = queues.get(queueName);
		if (queue!=null) {
			setConsumers(consumer, numberOfConsumers, queue);
		}
	}
	
	public <T> void addBatchListener(String queueName, Consumer<List<T>> consumer, int numberOfConsumers) {
		InMemoryQueue<T> queue = queues.get(queueName);
		if (queue!=null) {
			setBatchConsumers(consumer, numberOfConsumers, queue);
		}
	}
	
	
	/**
	 * Add message. If required create queue with no data.
	 * WARN - This should be used with thorough analysis as in case you passed wrong name for adding, it will create extra queue.
	 * @param queueName
	 * @param item
	 */
	synchronized public <T> void addAndCreate(String queueName, T item) {
		InMemoryQueue<T> imq = registerQueueIfAbsent(queueName);
		imq.add(item);
	}

	/**
	 * Get number of consumers currently attached to queue.
	 * @param queueName Name of queue for which consumers count is required
	 * @return Number of consumers
	 */
	public Integer getDirectConsumerCount(String queueName) {
		return queues.get(queueName).getDirectConsumerCount();
	}

	public void setConsumerCount(String queueName, int consumerCount) {
		queues.get(queueName).setDirectConsumerCount(consumerCount);
	}
	
	public void setBatchConsumerCount(String queueName, int i) {
		queues.get(queueName).setBatchConsumerCount(i);
	}
	
	/**
	 * This method makes queue dynamic. By dynamic it means that number of consumer will increase and reduce on basis of load.
	 * This method returns DynamicSettings by which one can configure particular queue behavior.
	 * @param queueName Queue Name
	 * @return Dynamic settings
	 */
	public <T> DynamicSettings<T> setDynamic(String queueName) {
		InMemoryQueue<T> queue = queues.get(queueName);
		DynamicSettings<T> settings = new DynamicSettings<T>().setQueue(queue);
		this.dynamicSettings.put(queueName, settings);
		return settings;
	}
	
	
}
