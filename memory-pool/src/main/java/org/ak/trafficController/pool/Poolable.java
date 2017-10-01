package org.ak.trafficController.pool;

public interface Poolable {
	default void clean() {};
	default void addBackToPool() {
		ObjectPoolManager.getInstance().addBackToPool(this);
	}
}
