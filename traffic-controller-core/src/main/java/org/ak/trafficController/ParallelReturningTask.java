package org.ak.trafficController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.pool.ObjectPoolManager;

public class ParallelReturningTask<T> extends ParallelTask<T> {

	static Logger logger = Logger.getLogger(ParallelReturningTask.class.getName());
	static <K> ParallelReturningTask<K> getFromPool(int unique, TaskType taskType, Supplier<K>... suppliers) {
		ParallelReturningTask<K> et = getPoolManager().getFromPool(ParallelReturningTask.class, ()->new ParallelReturningTask<K>(unique,taskType));
//		System.out.println("retrieved from pool...." + et.hashCode());
		et.uniqueNumber = unique;
		et.tasks.clear();
		if (taskType == TaskType.NORMAL) et.addSuppliers(suppliers);
		try {
		if (taskType == TaskType.SLOW) et.addSlowSuppliers(suppliers);
		} catch (RuntimeException e) {
			logger.log(Level.WARNING, "exception occured for " + unique, e);
			throw e;
		}
		et.taskType = taskType;
		et.startingTask = et;
		return et;
	}

	protected ConcurrentLinkedQueue<T> clq = ObjectPoolManager.getInstance().
			getFromPool(ConcurrentLinkedQueue.class, ConcurrentLinkedQueue<T>::new);
	
	public void addSuppliers(Supplier<T>... suppliers) {
		for (Supplier<T> supplier: suppliers) {
			tasks.add(ReturningTask.getFromPool(this.uniqueNumber,()->executeSupplier(supplier), TaskType.NORMAL));
		}
	}

	public T executeSupplier(Supplier<T> supplier) {
		T output = supplier.get();
		clq.add(output);
		postTaskRun();
		return output;
	}
	
	public void addSlowSuppliers(Supplier<T>... suppliers) {
		ReturningTask<T> taskFromPool;
		for (Supplier<T> supplier: suppliers) {
			try {
				taskFromPool = ReturningTask.getFromPool(uniqueNumber, ()->executeSupplier(supplier), TaskType.SLOW);
			} catch (RuntimeException re) {
				logger.log(Level.WARNING, "Exception for " + uniqueNumber, re);
				throw (re);
			}
			tasks.add(taskFromPool);
		}
	}

	
	public ParallelReturningTask(int uniqueId, TaskType taskType, Supplier<T>... suppliers) {
		super(uniqueId,taskType);
		addSuppliers(suppliers);
	}
	
	public <K> ReturningTask<K> join(Function<List<T>,K> merger) {
		ReturningTask<K> returningTask = ReturningTask.getFromPool(uniqueNumber,()->{
			List<T> list = get();
			K op =  merger.apply(list);
			return op;
		}, TaskType.NORMAL);
		then(returningTask);
		return returningTask;
	}

	public List<T> get() {
		List<T> list = new ArrayList<T>(this.clq);
		clq.clear();
		addBackToPool();
		return list;
	}

	public static ObjectPoolManager getPoolManager() {
		return ObjectPoolManager.getInstance();
	}
	
	public <K> ReturningTask<K> joinSlow(Function<List<T>,K> merger) {
		ReturningTask<K> returningTask = ReturningTask.getFromPool(uniqueNumber,()->{
			List<T> list = get();
			return merger.apply(list);
		}, TaskType.SLOW);
		then(returningTask);
		return returningTask;
	}
	
	public ExecutableTask join(Consumer<List<T>> merger) {
		return join(merger, TaskType.NORMAL);
	}

	protected ExecutableTask join(Consumer<List<T>> merger, TaskType tp) {
		ExecutableTask returningTask = getJoiningTask(merger, tp);
		then(returningTask);
		return returningTask;
	}

	protected ExecutableTask getJoiningTask(Consumer<List<T>> merger, TaskType tp) {
		return ExecutableTask.getFromPool(uniqueNumber,()->{
			List<T> list = get();
			merger.accept(list);
		}, tp);
	}
	
	public ExecutableTask joinSlow(Consumer<List<T>> merger) {
		return join(merger, TaskType.SLOW);
	}
	
	@Override
	public boolean canSendBackToPool() {
		return false;
	}
}
