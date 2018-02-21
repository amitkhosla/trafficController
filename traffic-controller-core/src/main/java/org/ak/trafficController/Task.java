package org.ak.trafficController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.pool.Poolable;

public abstract class Task implements Poolable {
	Logger logger = Logger.getLogger(Task.class.getName());
	protected Task startingTask;
	protected Integer uniqueNumber;
	protected TaskExecutor taskExecutor;
	protected Throwable throwable; //This is for throwing back in case of sync tasks and any task throw exception.
	
	protected Task parentTask; //The task which is parent of this sub chain.
	
	public enum TaskType {
		SLOW, NORMAL, NOTIFY
	}
	protected TaskType taskType;
	protected Task nextTask;
	protected Task exceptionTask;
	private boolean shouldThrowException;
	protected boolean shouldContinueOnException;
	
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

	abstract protected void executeCurrentTask() throws Throwable;
	
	public Task(int unique,TaskType taskType) {
		startingTask = this;
		this.taskType = taskType;
		this.uniqueNumber = unique;
	}
	
	protected void executeInternal() throws Throwable {
		executeCurrentTask();
		executeNextTask();
	}
	
	protected void execute() {
		try {
			executeInternal();
		} catch (Throwable throwable) {
			logger.log(Level.WARNING, "Exception occured in executing a task", throwable);
			this.throwable = throwable;
			boolean shouldThrow = false;
			if (Objects.isNull(exceptionTask)) {
				shouldThrow = true;
			}
			else {
				shouldThrow = this.shouldThrowException;
				this.exceptionTask.execute();
			}
			if (!shouldThrow && shouldContinueOnException) {
				executeNextTask();
				return;
			}
			
			throwIfRequired(throwable, shouldThrow);
		}
	}

	protected void throwIfRequired(Throwable throwable, boolean shouldThrow) {
		Task task = this;
		while (!Objects.isNull(task.nextTask)) {
			task = task.nextTask;
		}
		if (task.taskType == TaskType.NOTIFY) {
			if (shouldThrow) {
				task.throwable = throwable;
			} //won't set throwable if already handled.
			task.execute();
		}
		cleanAllRemainingTasks();
	}

	public Task shouldThrowExceptionIfOccurs() {
		this.shouldThrowException = true;
		return this;
	}
	
	public Task shouldContinueNextTaskIfExceptionOccurs() {
		this.shouldContinueOnException = true;
		return this;
	}
	
	protected void cleanAllRemainingTasks() {
		List<Task> tasks = new ArrayList<>(); 
		Task task = this;
		while (!Objects.isNull(task.nextTask)) {
			tasks.add(task);
			task = task.nextTask;
		}
		for (Task t : tasks) {
			t.nextTask = null;
		}
		tasks.clear();
	}

	protected void executeNextTask() {
		if (nextTask != null) {
			if (nextTask.taskExecutor == null) {
				nextTask.taskExecutor = taskExecutor;
			}
			nextTask.taskExecutor.enque(nextTask);
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
	 * Throwable is thrown if any task execution throws any exception. The user can also handle the exception by adding onException method call.
	 * @param timeToWait Max time we will wait for the response.
	 * @throws Throwable Throwable is thrown if any method execution fails
	 */
	public void start(long timeToWait) throws Throwable {
		NotifyingTask task = new NotifyingTask(uniqueNumber);
		then(task);
		doSubmit();
		pauseExecutingThread(task, timeToWait);
	}
	
	public void start() throws Throwable {
		NotifyingTask task = new NotifyingTask(uniqueNumber);
		then(task);
		doSubmit();
		pauseExecutingThread(task, 5*60*60);
	}
	
	public ExecutableTask onException(Consumer<Throwable> exceptionHandler) {
		RunnableToBeExecuted runnable = ()-> exceptionHandler.accept(this.throwable);
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,runnable, TaskType.NORMAL);
		setExceptionTaskParams(task);
		return task;
	}
	
	public <T> ReturningTask<T> onExceptionGet(Function<Throwable, T> exceptionHandler) {
		Supplier<T> supplier = ()->exceptionHandler.apply(throwable);
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber, supplier, TaskType.NORMAL);
		setExceptionTaskParams(task);
		return task;
	}
	
	public ExecutableTask onExceptionPerfomAndAlsoContinueOtherTasks(Consumer<Throwable> exceptionHandler) {
		RunnableToBeExecuted runnable = ()-> exceptionHandler.accept(this.throwable);
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,runnable, TaskType.NORMAL);
		setExceptionTaskParams(task);
		task.shouldContinueOnException = true;
		return task;
	}
	
	public <T> ReturningTask<T> onExceptionGetAndAlsoContinueOtherTasks(Function<Throwable, T> exceptionHandler) {
		Supplier<T> supplier = ()->exceptionHandler.apply(throwable);
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber, supplier, TaskType.NORMAL);
		setExceptionTaskParams(task);
		
		return task;
	}

	protected void setExceptionTaskParams(Task task) {
		task.parentTask = this;
		this.exceptionTask = task;
		task.taskExecutor = taskExecutor;
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
	
	protected void pauseExecutingThread(NotifyingTask task, long timeToWait) throws Throwable {
		synchronized(task) {
			try {
				task.wait(timeToWait);
			} catch (InterruptedException e) {
				logger.log(Level.WARNING, "could not wait....", e);
			}
			
			if (!Objects.isNull(task.throwable)) {
				throw task.throwable;
			}
		
			if (!task.executed) {
				throw new RuntimeException("Could not process in time");
			}
		}
	}

	public void submit() {
		doSubmit();
	}

	protected void doSubmit() {
		Task task;
		if (Objects.isNull(this.parentTask)) {
			task = this.startingTask;
		} else {
			task = this.parentTask;
			while (!Objects.isNull(task.parentTask)) {
				task = task.parentTask;
			}
			task = task.startingTask;
		}
		try {
		taskExecutor.enque(task);
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
		task.parentTask = this.parentTask;
		return task;
	}
	
	public <T> ReturningTask<T> then(Supplier<T> supplier) {
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber, supplier, TaskType.NORMAL);
		then(task);
		return task;
	}
	
	public Task then(RunnableToBeExecuted runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,runnable, TaskType.NORMAL);
		then(task);
		return task;
	}
	
	public <T> ReturningTask<T> thenSlow(Supplier<T> supplier) {
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber,supplier, TaskType.SLOW);
		then(task);
		return task;
	}
	
	public Task thenSlow(RunnableToBeExecuted runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber, runnable, TaskType.SLOW);
		then(task);
		return task;
	}
	
	public <T> ParallelExecutingTask<T> thenParallel(TaskType tp, Collection<T> collection, Consumer<T> consumerToWorkOn) {
		int size = 0;
		if (!Objects.isNull(collection)) {
			size = collection.size();
		}
		RunnableToBeExecuted[] runnables = new RunnableToBeExecuted[size];
		if (size > 0) {
			int i =0;
			for (T item : collection) {
				runnables[i++] = ()-> consumerToWorkOn.accept(item);
			}
		}
		return thenParallelWithoutWait(tp, runnables);
	}
	
	public <T> ParallelExecutingTask<T> thenParallel(TaskType tp, T itemToWorkOn, Collection<Consumer<T>> consumersToWorkOn) {
		int size = 0;
		if (!Objects.isNull(consumersToWorkOn)) {
			size = consumersToWorkOn.size();
		}
		RunnableToBeExecuted[] runnables = new RunnableToBeExecuted[size];
		if (size > 0) {
			int i =0;
			for (Consumer<T> consumer : consumersToWorkOn) {
				runnables[i++] = ()-> consumer.accept(itemToWorkOn);
			}
		}
		return thenParallel(tp, runnables);
	}
	
	public <T> ParallelExecutingTask<T> thenParallelAsync(TaskType tp, Collection<T> collection, Consumer<T> consumerToWorkOn) {
		int size = 0;
		if (!Objects.isNull(collection)) {
			size = collection.size();
		}
		RunnableToBeExecuted[] runnables = new RunnableToBeExecuted[size];
		if (size > 0) {
			int i =0;
			for (T item : collection) {
				runnables[i++] = ()-> consumerToWorkOn.accept(item);
			}
		}
		return thenParallelWithoutWait(tp, runnables);
	}
	
	public <T, K> ParallelReturningTask<T> thenParallelReturning(TaskType tp, K itemToWorkOn, Collection<Function<K, T>> functions) {
		Supplier<T> suppliers[] = new Supplier[functions.size()];
		int i = 0;
		for (Function<K,T> function : functions) {
			suppliers[i++] = ()->function.apply(itemToWorkOn);
		}
		return thenParallel(tp, suppliers);
	}
	
	public <T, K> ParallelReturningTask<T> thenParallelReturningAsync(TaskType tp, K itemToWorkOn, Collection<Function<K, T>> functions) {
		Supplier<T> suppliers[] = new Supplier[functions.size()];
		int i = 0;
		for (Function<K,T> function : functions) {
			suppliers[i++] = ()->function.apply(itemToWorkOn);
		}
		return thenParallelWithoutWait(tp, suppliers);
	}
	
	public <T> ParallelExecutingTask<T> thenParallelAsync(TaskType tp, T itemToWorkOn, Collection<Consumer<T>> consumersToWorkOn) {
		int size = 0;
		if (!Objects.isNull(consumersToWorkOn)) {
			size = consumersToWorkOn.size();
		}
		RunnableToBeExecuted[] runnables = new RunnableToBeExecuted[size];
		if (size > 0) {
			int i =0;
			for (Consumer<T> consumer : consumersToWorkOn) {
				runnables[i++] = ()-> consumer.accept(itemToWorkOn);
			}
		}
		return thenParallelWithoutWait(tp, runnables);
	}
	
	public <T> ParallelExecutingTask<T> thenParallelWithoutWait(TaskType tp, RunnableToBeExecuted... runnables) {
		ParallelExecutingTask parallelExecutingTask = ParallelExecutingTask.getFromPool(uniqueNumber, tp, runnables);
		UnlinkedTask task = new UnlinkedTask(uniqueNumber, tp, parallelExecutingTask);
		includeUnlinkTask(parallelExecutingTask, task);
		return parallelExecutingTask;
	}

	protected void includeUnlinkTask(Task parallelExecutingTask, UnlinkedTask task) {
		then(task);
		parallelExecutingTask.taskExecutor = taskExecutor;
	}
	
	public <T> ParallelExecutingTask<T> thenParallelWithoutWait(RunnableToBeExecuted... runnables) {
		return thenParallelWithoutWait(TaskType.NORMAL, runnables);
	}
	
	public <T> ParallelExecutingTask<T> thenParallelWithoutWaitSlow(RunnableToBeExecuted... runnables) {
		return thenParallelWithoutWait(TaskType.SLOW, runnables);
	}

	public <T> ParallelExecutingTask<T> thenParallel(RunnableToBeExecuted... runnables) {
		return thenParallel(TaskType.NORMAL, runnables);
	}

	public <T> ParallelExecutingTask<T> thenParallel(TaskType tp, RunnableToBeExecuted... runnables) {
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
	
	public <T> ParallelReturningTask<T> thenParallelWithoutWait(TaskType tp, Supplier<T>... suppliers) {
		ParallelReturningTask<T> parallelReturningTask = ParallelReturningTask.getFromPool(uniqueNumber,tp,suppliers);
		UnlinkedTask task = new UnlinkedTask(uniqueNumber, tp, parallelReturningTask);
		includeUnlinkTask(parallelReturningTask, task);
		return parallelReturningTask;
	}
	
	public <T> ParallelExecutingTask<T> thenParallelSlow(RunnableToBeExecuted... runnables) {
		return thenParallel(TaskType.SLOW, runnables);
	}
	
	public <T> ParallelReturningTask<T> thenParallelSlow(Supplier<T>... suppliers) {
		return thenParallel(TaskType.SLOW, suppliers);
	}

	public Task getParentTask() {
		return this.parentTask;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Task [type: " + this.getClass() + ", uniqueNumber: " + uniqueNumber + ", taskType : " + taskType + "]";
	}

	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}
}
