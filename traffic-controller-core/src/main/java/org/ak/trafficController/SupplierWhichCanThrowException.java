package org.ak.trafficController;

import java.util.function.Supplier;

/**
 * {@link Supplier} could have been used for majority of cases but if some method throws some exception, we need to catch it there itself. 
 * To overcome this, this new interface will be able to handle such scenarios. 
 * @author amit.khosla
 *
 * @param <T>
 */
public interface SupplierWhichCanThrowException<T> {
	T get() throws Throwable;
}
