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
 * @author Amit Khosla
 */
public class MultiplexInMemoryQueueManager {

	private Map<String, Consumer<MuxWrapper>> consumersMap = new ConcurrentHashMap<>();
	
	private InMemoryQueue<MuxWrapper> muxQueue = new InMemoryQueue<>("muxQueue");
	
	private Map<String, List<MuxWrapper>> missedMuxs = new ConcurrentHashMap<>();
	
	private InMemoryQueueTuner tuner = new InMemoryQueueTuner();
	
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

	public <T> void register(String key, Consumer<T> consumer) {
		consumersMap.put(key, getConsumer(consumer));
		enqueMissedMuxIfAny(key);
	}

	protected void enqueMissedMuxIfAny(String key) {
		List<MuxWrapper> list = missedMuxs.get(key);
		if (!CollectionUtils.isEmpty(list)) {
			muxQueue.addAllFromList(list);
		}
	}

	protected <T> Consumer<MuxWrapper> getConsumer(Consumer<T> consumer) {
		return muxWrapper->consumer.accept((T) muxWrapper.getObj());
	}
	
	/**
	 * Set dynamic nature
	 * @return
	 */
	public DynamicSettings<MuxWrapper> setDynamicSettings() {
		DynamicSettings<MuxWrapper> dynamicSettings = new DynamicSettings<MuxWrapper>().setQueue(muxQueue);
		tuner.dynamicSettings.add(dynamicSettings);
		tuner.startTuning();
		return dynamicSettings;
	}

	public <T> void add(String key, T item) {
		muxQueue.add(new MuxWrapper().setObj(item).setKey(key));
	}

	public <T> void addMultiple(String key, List<T> list) {
		list.forEach(item->muxQueue.add(new MuxWrapper().setKey(key).setObj(item)));
	}
	
}
