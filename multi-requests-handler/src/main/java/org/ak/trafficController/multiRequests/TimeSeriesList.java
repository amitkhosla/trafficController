/**
 * 
 */
package org.ak.trafficController.multiRequests;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.ak.trafficController.messaging.mem.InMemoryQueue;

/**
 * The class will be used to keep list of items for each time unit.
 * This will be a data structure, which will keep things segregated.
 * @author amit.khosla
 *
 */
public class TimeSeriesList {
	boolean shouldKeepCleaning = true;
	TreeMap<LocalDateTime, ConcurrentLinkedQueue<MultiRequestDTO>> items = new TreeMap<>();

	// Thread doing the cleanup from the cache.
	Thread cleanupThread = new Thread(()-> {
		while (shouldKeepCleaning) {
			LocalDateTime currentTime = LocalDateTime.now();
			List<ConcurrentLinkedQueue<MultiRequestDTO>> itemsToBeCleaned = new ArrayList<>();
			synchronized (items) {
				LocalDateTime timeEq = MultiRequestDTO.getApplicableLocalDateTime(currentTime);
				Iterator<Entry<LocalDateTime, ConcurrentLinkedQueue<MultiRequestDTO>>> iter = items.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<LocalDateTime, ConcurrentLinkedQueue<MultiRequestDTO>> entry = iter.next();
					if (entry.getKey().isAfter(timeEq)) {
						break;
					} else {
						itemsToBeCleaned.add(entry.getValue());
						iter.remove();
					}
				}
			}
			itemsToBeCleaned.forEach(this::process);
			sleepForSeconds(1);
		}
	});
	
	private Consumer<MultiRequestDTO> consumer = t->{};
	private String name;

	InMemoryQueue<MultiRequestDTO> addedItemsQueue = new InMemoryQueue<>("processingQueue" + name);
	InMemoryQueue<MultiRequestDTO> processingQueue = new InMemoryQueue<>("processingQueue1" + name);
	
	public TimeSeriesList(String name) {
		this.name = name;
		initialize();
	}
	
	/**
	 * Sleep for given amount of seconds.
	 * @param secondsToSleep
	 */
	private void sleepForSeconds(long secondsToSleep) {

		try {
			Thread.sleep(1000l * secondsToSleep);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Process the DTOs in async fashion by adding them to queue.
	 * @param concurrentLinkedQueue The queue from where we are looking for the data
	 */
	protected void process(ConcurrentLinkedQueue<MultiRequestDTO> concurrentLinkedQueue) {
		//System.out.println("about to add items in processing queue ... size " + concurrentLinkedQueue.size());
		if (concurrentLinkedQueue != null && !concurrentLinkedQueue.isEmpty()) {
			processingQueue.addAllFromCollection(concurrentLinkedQueue);
		}
		
	}

	public void setConsumer(Consumer<MultiRequestDTO> consumer) {
		this.consumer = consumer;
	}
	
	public void initialize() {
		addedItemsQueue.setDirectConsumer(item-> {
			addItemToItems(item);
		}).setDirectConsumerCount(5);
		processingQueue.setDirectConsumer(t->{
			consumer.accept(t);
		}).setDirectConsumerCount(5);
		cleanupThread.start();
	}
	
	/**
	 * Add item to the times.
	 * @param item Item to be added
	 */
	protected void addItemToItems(MultiRequestDTO item) {
		ConcurrentLinkedQueue<MultiRequestDTO> clq = items.get(item.getExpiryTime());
		if (clq == null) {
			synchronized (items) {
				clq = items.get(item.getExpiryTime());
				if (clq == null) {
					clq = new ConcurrentLinkedQueue<>();
					items.put(item.getExpiryTime(), clq);
				}
			}
		}
		clq.add(item);
	}

	public void add(MultiRequestDTO item) {
		addedItemsQueue.add(item);
	}
	
}
