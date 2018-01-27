package org.ak.trafficController.pool;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ObjectPool<T> {
	
	private Supplier<T> generator;
	
	private AtomicInteger count = new AtomicInteger();
	
	private int poolSize = 2000;
	
	public ObjectPool(Supplier<T> generator) {
		if (generator == null) throw new RuntimeException("Generator cant be null");
		this.generator = generator;
	}
	
	private ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<T>();
	
	public void addBackToPool(T obj) {
		if (count.get() < poolSize) {
			count.incrementAndGet();
			pool.add(obj);
		} //else ignore.
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
	
	public ObjectPool<T> setSize(int size) {
		this.poolSize = size;
		return this;
	}
	
}
