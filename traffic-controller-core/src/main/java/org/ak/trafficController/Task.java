package org.ak.trafficController;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.pool.Poolable;

public abstract class Task implements Poolable {
	Logger logger = Logger.getLogger(Task.class.getName());
	protected Task startingTask;
	protected Integer uniqueNumber;
	protected TaskExecutor taskExecutor;
	public enum TaskType {
		SLOW, NORMAL, NOTIFY
	}
	protected TaskType taskType;
	protected Task nextTask;
	
	@Override
	public void clean() {
		logger.finer("about to clean up " + this.hashCode() + " " + this.getClass() + " " + this.taskType + " for " + uniqueNumber + getStackTrace());
		taskType = null;
		startingTask = null;
		nextTask = null;
		uniqueNumber = null;
	}
	
	private String getStackTrace() {
		StringBuilder sb = new StringBuilder();
		for(StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			sb.append(ste.getClassName()).append(" ").append(ste.getLineNumber()).append(",");
		}
		return sb.toString();
	}

	abstract protected void executeCurrentTask();
	
	public Task(int unique,TaskType taskType) {
		startingTask = this;
		this.taskType = taskType;
		this.uniqueNumber = unique;
	}
	
	protected void execute() {
		executeCurrentTask();
		executeNextTask();
	}

	protected void executeNextTask() {
		if (nextTask != null) {
			taskExecutor.enque(nextTask);
		}
		if (canSendBackToPool()) {
			this.addBackToPool();
		}
	}
	
	public boolean canSendBackToPool() {
		return this.startingTask != this;
	}

	/**
	 * This will start the execution and calling thread will wait for execution.
	 */
	public void start() {
		NotifyingTask task = new NotifyingTask(uniqueNumber);
		then(task);
		doSubmit();
		pauseExecutingThread(task);
	}
	
/*	protected void notifyBack() {
		ArrayBlockingQueue abq = map.get(this.uniqueNumber);
		if (abq==null) {
			map.putIfAbsent(this.uniqueNumber, ObjectPoolManager.getInstance().getFromPool(ArrayBlockingQueue.class, ()->new ArrayBlockingQueue(1)));
		}
		abq=map.get(uniqueNumber);
		try {
			abq.add(new Object());
		} catch(RuntimeException re) {
			System.err.println("Error occured for ... " + monitor + ">>>>" + uniqueNumber);
			throw re;
		}
	}*/
	
	protected void pauseExecutingThread(NotifyingTask task) {
		synchronized(task) {
			try {
				task.wait();
			} catch (InterruptedException e) {
				logger.log(Level.WARNING, "could not wait....", e);
			}
		}
	}

	public void submit() {
		doSubmit();
	}

	protected void doSubmit() {
		try {
		taskExecutor.enque(this.startingTask);
		} catch (Exception e) {
			System.err.println(this);
			e.printStackTrace();
		}
	}
	
	public Task then(Task task) {
		this.nextTask = task;
		task.startingTask = this.startingTask;
		task.uniqueNumber = this.uniqueNumber;
		task.taskExecutor = this.taskExecutor;
		return task;
	}
	
	public <T> ReturningTask<T> then(Supplier<T> supplier) {
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber, supplier, TaskType.NORMAL);
		then(task);
		return task;
	}
	
	public Task then(Runnable runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,runnable, TaskType.NORMAL);
		then(task);
		return task;
	}
	
	public <T> ReturningTask<T> thenSlow(Supplier<T> supplier) {
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber,supplier, TaskType.SLOW);
		then(task);
		return task;
	}
	
	public Task thenSlow(Runnable runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber, runnable, TaskType.SLOW);
		then(task);
		return task;
	}
	
	public <T> ParallelExecutingTask<T> thenParallel(Runnable... runnables) {
		return thenParallel(TaskType.NORMAL, runnables);
	}

	public <T> ParallelExecutingTask<T> thenParallel(TaskType tp, Runnable... runnables) {
		ParallelExecutingTask<T> parallelExecutingTask = ParallelExecutingTask.getFromPool(uniqueNumber, tp, runnables);
		then(parallelExecutingTask);
		return parallelExecutingTask;
	}
	
	public <T> ParallelReturningTask<T> thenParallel(Supplier<T>... suppliers) {
		ParallelReturningTask<T> parallelExecutingTask = thenParallel(TaskType.NORMAL, suppliers);
		return parallelExecutingTask;
	}

	public <T> ParallelReturningTask<T> thenParallel(TaskType tp, Supplier<T>... suppliers) {
		ParallelReturningTask<T> parallelExecutingTask = ParallelReturningTask.getFromPool(uniqueNumber,tp,suppliers);
		then(parallelExecutingTask);
		return parallelExecutingTask;
	}
	
	public <T> ParallelExecutingTask<T> thenParallelSlow(Runnable... runnables) {
		return thenParallel(TaskType.SLOW, runnables);
	}
	
	public <T> ParallelReturningTask<T> thenParallelSlow(Supplier<T>... suppliers) {
		return thenParallel(TaskType.SLOW, suppliers);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Task [type: " + this.getClass() + ", uniqueNumber: " + uniqueNumber + ", taskType : " + taskType + "]";
	}

	
}
