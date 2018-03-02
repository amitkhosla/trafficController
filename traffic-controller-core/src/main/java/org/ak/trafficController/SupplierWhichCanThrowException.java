package org.ak.trafficController;

public interface SupplierWhichCanThrowException<T> {
	T get() throws Throwable;
}
