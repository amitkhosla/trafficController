/**
 * 
 */
package org.ak.trafficController;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class contains details of what needs to be called for each task.
 * @author amit.khosla
 *
 */
public class ThreadingDetails<T> {
	/**
	 * Object which is retrieved from main flow.
	 */
	private T objectFromMainFlow;
	
	/**
	 * In case the data needs to be set from some supplier. If object is not set then only this supplier will be called.
	 */
	private Supplier<T> supplierWhichWillSetObjectFromMainThread; 
	
	/**
	 * Consumer of the thread data which will be responsible for setting thread locals and will run pre running any task.
	 */
	private Consumer<T> processingForEachThread;
	/**
	 * Consumer of thread data which will be responsible of cleaning the thread locals set and will run post running any task.
	 */
	private Consumer<T> cleaner;
	
	/**
	 * Retrieves the data which was set either by supplier or the data itself.
	 * @return Data which will be set in consecutive tasks
	 */
	public T getObjectFromMainFlow() {
		if (objectFromMainFlow == null) {
			objectFromMainFlow = supplierWhichWillSetObjectFromMainThread.get();
		}
		return objectFromMainFlow;
	}
	
	/**
	 * Set object from main flow.
	 * @param objectFromMainFlow Object to be set
	 * @return Self for further use
	 */
	public ThreadingDetails<T> setObjectFromMainFlow(T objectFromMainFlow) {
		this.objectFromMainFlow = objectFromMainFlow;
		return this;
	}
	
	/**
	 * Get processing consumer which will be run on thread data to set thread locals.
	 * @return Processing consumer which will be run on thread data to set thread locals
	 */
	public Consumer<T> getProcessingForEachThread() {
		return processingForEachThread;
	}
	/**
	 * Set processing consumer which will be run on thread data to set thread locals.
	 * @param processingForEachThread Consumer of thread data which will be run on thread data to set thread locals
	 * @return Self for further use
	 */
	public ThreadingDetails<T> setProcessingForEachThread(Consumer<T> processingForEachThread) {
		this.processingForEachThread = processingForEachThread;
		return this;
	}
	/**
	 * Get cleaner consumer of thread data which will remove all thread locals. 
	 * @return Cleaner consumer
	 */
	public Consumer<T> getCleaner() {
		return cleaner;
	}
	/**
	 * Set Cleaner consumer of thread data which will remove all thread locals. 
	 * @param cleaner consumer of thread data which will remove all thread locals. 
	 * @return Self for further use
	 */
	public ThreadingDetails<T> setCleaner(Consumer<T> cleaner) {
		this.cleaner = cleaner;
		return this;
	}
	/**
	 * Get supplier which will set object from main thread.
	 * @return the supplierWhichWillSetObjectFromMainThread
	 */
	public Supplier<T> getSupplierWhichWillSetObjectFromMainThread() {
		return supplierWhichWillSetObjectFromMainThread;
	}
	/**
	 * This method sets the supplier.
	 * @param supplierWhichWillSetObjectFromMainThread the supplierWhichWillSetObjectFromMainThread to set
	 * @return Self for further use
	 */
	public ThreadingDetails<T> setSupplierWhichWillSetObjectFromMainThread(Supplier<T> supplierWhichWillSetObjectFromMainThread) {
		this.supplierWhichWillSetObjectFromMainThread = supplierWhichWillSetObjectFromMainThread;
		return this;
	}
	
	/**
	 * Set threading details. This method sets all thread locals requested. 
	 * @return self for further use
	 */
	public ThreadingDetails<T> setThreadingDetails() {
		getProcessingForEachThread().accept(getObjectFromMainFlow());
		return this;
	}
	
	/**
	 * Cleaning task which removes all thread locals from the task post execution.
	 * @return Self for further use
	 */
	public ThreadingDetails<T> clean() {
		cleaner.accept(getObjectFromMainFlow());
		return this;
	}
}
