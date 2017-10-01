package org.ak.trafficController.pool;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class ObjectPool<T> {
	private Supplier<T> generator;
	public ObjectPool(Supplier<T> generator) {
		if (generator == null) throw new RuntimeException("Generator cant be null");
		this.generator = generator;
	}
	private ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<T>();
	public void addBackToPool(T obj) {
		pool.add(obj);
	}
	public T getFromPool() {
		T obj = pool.poll();
		if (obj == null) {
			return generator.get();
		}
		return obj;
	}
	public int getCount() {
		return pool.size();
	}
	
}
