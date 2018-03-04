package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.ak.trafficController.messaging.mem.wrapper.MuxWrapper;
import org.apache.commons.collections.CollectionUtils;

/**
 * This class is for having single set of consumers for all in memory queue.
 * There can be a requirement where we want only one queue to be used and having all settings done for this queue.
 * The consumers will be registered and based on the object, data will be picked.
 * @author Amit Khosla
 */
public class MultiplexInMemoryQueueManager {

	/**
	 * Different consumers which will be used for given type of data.
	 */
	private Map<String, Consumer<MuxWrapper>> consumersMap = new ConcurrentHashMap<>();
	
	/**
	 * Actual queue which will be used.
	 */
	private InMemoryQueue<MuxWrapper> muxQueue = new InMemoryQueue<>("muxQueue");
	
	/**
	 * Data having missed key. Might be at start we do not have consumer added for it.
	 */
	private Map<String, List<MuxWrapper>> missedMuxs = new ConcurrentHashMap<>();
	
	/**
	 * Tuner which will be used.
	 */
	private InMemoryQueueTuner tuner = new InMemoryQueueTuner();
	
	/**
	 * Constructor
	 */
	public MultiplexInMemoryQueueManager() {
		init();
	}

	protected void init() {
		Consumer<MuxWrapper> consumer = mux-> {
			Consumer<MuxWrapper> wrappedConsumer = consumersMap.get(mux.getKey());
			if (wrappedConsumer != null) {
				wrappedConsumer.accept(mux);
			} else {
				putInMissedMap(mux);
			}
		};
		muxQueue.setDirectConsumer(consumer);
	}
	
	/**
	 * Put in missed map for any key for which consumer is not set yet.
	 * @param mux Mux which will be put in the missed items
	 */
	private void putInMissedMap(MuxWrapper mux) {
		String key = mux.getKey();
		List<MuxWrapper> list = missedMuxs.get(key);
		if (list == null) {
			synchronized (missedMuxs) {
				if (list == null) {
					list = new ArrayList<>();
					missedMuxs.put(key, list);
				}
			}
		}
		list.add(mux);
	}

	/**
	 * Register a consumer against a key. All data having this key will be processed using this consmer.
	 * @param key Key of data
	 * @param consumer Consumer logic
	 * @param <T> type of consumer
	 */
	public <T> void register(String key, Consumer<T> consumer) {
		consumersMap.put(key, getConsumer(consumer));
		/// TODO - Should not we call the consumer directly here?
		enqueMissedMuxIfAny(key);
	}

	/**
	 * Enqueue all the missed items back to the queue.
	 * @param key Key for which any missing items is processed
	 */
	protected void enqueMissedMuxIfAny(String key) {
		List<MuxWrapper> list = missedMuxs.get(key);
		if (!CollectionUtils.isEmpty(list)) {
			muxQueue.addAllFromCollection(list);
		}
	}

	/**
	 * Get the consumer of queue which will process the actual paased consumer.
	 * @param consumer Actual consumer logic
	 * @return Consumer of queue
	 * @param <T> type of consumer
	 */
	protected <T> Consumer<MuxWrapper> getConsumer(Consumer<T> consumer) {
		return muxWrapper->consumer.accept((T) muxWrapper.getObj());
	}
	
	/**
	 * Set dynamic nature for this queue.
	 * @return Dynamic settings to further tune it
	 */
	public DynamicSettings<MuxWrapper> setDynamicSettings() {
		DynamicSettings<MuxWrapper> dynamicSettings = new DynamicSettings<MuxWrapper>().setQueue(muxQueue);
		tuner.dynamicSettings.add(dynamicSettings);
		tuner.startTuning();
		return dynamicSettings;
	}

	/**
	 * Add item in the queue for given key.
	 * @param key Key for which this data is designated
	 * @param item Item to be added
	 * @param <T> type of data
	 */
	public <T> void add(String key, T item) {
		muxQueue.add(new MuxWrapper().setObj(item).setKey(key));
	}

	/**
	 * Add multiple items in the queue for given queue.
	 * All items passed in collection will be added to the queue.
	 * @param key Key against which we want to add the data
	 * @param list Collection having data
	 * @param <T> type of data
	 */
	public <T> void addMultiple(String key, List<T> list) {
		list.forEach(item->muxQueue.add(new MuxWrapper().setKey(key).setObj(item)));
	}
	
}
