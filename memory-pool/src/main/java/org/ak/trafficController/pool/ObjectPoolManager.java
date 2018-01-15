package org.ak.trafficController.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.ak.trafficController.messaging.mem.InMemoryQueue;

public class ObjectPoolManager {
	
	static Logger LOGGER = Logger.getLogger(ObjectPoolManager.class.getName()); 
	
	Map<Class,ObjectPool> map = new HashMap<>();
	InMemoryQueue<Poolable> cleanupChannel = new InMemoryQueue<>("cleanup");
	public ObjectPoolManager() {
		init();
	}
	public void init() {
		cleanupChannel.setDirectConsumer(t->{
			t.clean();
			addBackToPoolGeneric(t);
		});
	}
	public <T> void addObjectPool(Class<T> c,ObjectPool<T> pool) {
		map.put(c, pool);
	}
	public <T> ObjectPool<T> getPool(Class<T> cls, Supplier<T> generator) {
		ObjectPool objectPool = map.get(cls);
		if (objectPool == null) {
			objectPool = new ObjectPool<T>(generator);
			map.put(cls, objectPool);
		}
		return objectPool;
	}
	public <T> T getFromPool(Class<T> cls, Supplier<T> generator) {
		return getPool(cls, generator).getFromPool();
	}
	
	public <T extends Poolable> void addBackToPool(T obj) {
		cleanupChannel.add(obj);
	}
	
	public <T> void addBackToPoolGeneric(T obj) {
		ObjectPool objectPool = map.get(obj.getClass());
		if (!Objects.isNull(objectPool)) { 
			objectPool.addBackToPool(obj);
		}
	}
	
	static ObjectPoolManager opm = new ObjectPoolManager();
	public static ObjectPoolManager getInstance() {
		return opm;
	}
	
	public void printStats() {
		this.map.forEach((Class k, ObjectPool v)->{
			LOGGER.info(k + " - " + v.getCount());
		});
	}
}
