package org.ak.trafficController;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.ak.trafficController.pool.ObjectPoolManager;

public class ReturningTask<T> extends Task {

	static <K> ReturningTask<K> getFromPool(int unique, SupplierWhichCanThrowException<K> supplier, TaskType taskType) {
		ReturningTask<K> et = ObjectPoolManager.getInstance().getFromPool(ReturningTask.class, ()->new ReturningTask<K>(unique, supplier, taskType));
		et.uniqueNumber = unique;
		et.supplier = supplier;
		et.taskType = taskType;
		et.startingTask = et;
		return et;
	}
	
	@Override
	public boolean canSendBackToPool() {
		return false;
	}
	
	private SupplierWhichCanThrowException<T> supplier;
	private T output;

	public ReturningTask(int unique, SupplierWhichCanThrowException<T> supplier, TaskType taskType) {
		super(unique, taskType);
		this.supplier = supplier;
	}
	
	public ExecutableTask thenConsume(Consumer<T> consumer) {
		ExecutableTask t = thenConsume(consumer, TaskType.NORMAL);
		return t;
	}
	
	public ParallelExecutingTask thenConsumeMultiple(Consumer<T>... consumers) {
		AtomicBoolean retrieved = new AtomicBoolean(false);
		AtomicReference<T> data = new AtomicReference<>(null);
		RunnableToBeExecuted[] runnables = new RunnableToBeExecuted[consumers.length];
		for (int i=0;i<consumers.length; i++) {
			Consumer<T> consumer = consumers[i];
			runnables[i] = ()-> {
					if (!retrieved.get()) {
						synchronized (retrieved) {
							if (!retrieved.get()) {	
								T val = this.get();
								data.set(val);
								retrieved.set(true);
							}
						}
					}
					consumer.accept(data.get());
			};
		}
		ParallelExecutingTask<T> parallelTask = ParallelExecutingTask.getFromPool(this.uniqueNumber, TaskType.NORMAL, runnables);
		then(parentTask);
		return parallelTask;
	}

	protected ExecutableTask thenConsume(Consumer<T> consumer, TaskType tp) {
		ExecutableTask t = getConsumeExecutableTask(consumer, tp);
		then(t);
		return t;
	}

	protected ExecutableTask getConsumeExecutableTask(Consumer<T> consumer,
			TaskType tp) {
		ExecutableTask t = ExecutableTask.getFromPool(uniqueNumber,()->{
			T t2 = this.get();
			consumer.accept(t2);
		}, tp);
		return t;
	}
	
	public ExecutableTask thenConsumeSlow(Consumer<T> consumer) {
		return thenConsume(consumer, TaskType.SLOW);
	}
	
	public <R> ReturningTask<R> then(Function<T,R> consumer) {
		return then(consumer, TaskType.NORMAL);
	}

	protected <R> ReturningTask then(Function<T, R> consumer, TaskType tp) {
		ReturningTask outputTask= ReturningTask.getFromPool(uniqueNumber,()->consumer.apply(this.get()), tp);
		super.then(outputTask);
		return outputTask;
	}
	
	public <R> ReturningTask<R> thenSlow(Function<T,R> consumer) {
		return then(consumer, TaskType.SLOW);
	}

	@Override
	protected void executeCurrentTask() throws Throwable {
		this.output = this.supplier.get();
	}
	
	public T get() {
		addBackToPool();
		return output;
	}

}
