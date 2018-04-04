package org.ak.trafficController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.pool.Poolable;

/**
 * Task is base class which let caller make small chunks which will run one by one in chain.
 * @author amit.khosla
 */
public abstract class Task implements Poolable {
	Logger logger = Logger.getLogger(Task.class.getName());
	
	List<ThreadingDetails> details = new ArrayList<>();
	
	protected Task startingTask;
	protected Integer uniqueNumber;
	protected TaskExecutor taskExecutor;
	protected Throwable throwable; //This is for throwing back in case of sync tasks and any task throw exception.

	protected String name;
	
	protected Task parentTask; //The task which is parent of this sub chain.
	
	public enum TaskType {
		SLOW, NORMAL, NOTIFY
	}
	protected TaskType taskType;
	protected Task nextTask;
	protected Task exceptionTask;
	private boolean shouldThrowException;
	protected boolean shouldContinueOnException;
	
	/* (non-Javadoc)
	 * @see org.ak.trafficController.pool.Poolable#clean()
	 */
	@Override
	public void clean() {
		logger.finer("about to clean up " + this.name + " " + this.hashCode() + " " + this.getClass() + " " + this.taskType + " for " + uniqueNumber + getStackTrace());
		taskType = null;
		startingTask = null;
		nextTask = null;
		uniqueNumber = null;
		details.clear();
	}
	
	/**
	 * Returns the stack trace for debug purposes.
	 * @return Stack trace as string
	 */
	private String getStackTrace() {
		StringBuilder sb = new StringBuilder();
		for(StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			sb.append(ste.getClassName()).append(" ").append(ste.getLineNumber()).append(",");
		}
		return sb.toString();
	}

	/**
	 * The current task is executed by this method. For each type of task it will behave differently.
	 * @throws Throwable In case any exception occurred in task, a Throwable will be thrown
	 */
	abstract protected void executeCurrentTask() throws Throwable;
	
	/**
	 * Constructor of Task to keep every task defined as a unique number and a given task type.
	 * @param unique Unique id
	 * @param taskType Task type
	 */
	public Task(int unique,TaskType taskType) {
		startingTask = this;
		this.taskType = taskType;
		this.uniqueNumber = unique;
	}
	
	/**
	 * This method is controlling the behavior of most of the tasks.
	 * We will execute current task and then execute next task (mostly adding to TaskExecutor)
	 * @throws Throwable Throws if any issue occur in processing task
	 */
	protected void executeInternal() throws Throwable {
		executeCurrentTask();
		executeNextTask();
	}
	
	/**
	 * This method is called from task executor for running the task.
	 * We try to handle the exception which if occur we throw the same to user.
	 */
	protected void execute() {
		try {
			details.forEach(ThreadingDetails::setThreadingDetails);
			TaskDetailsThreadLocal.setTask(this);
			executeInternal();
		} catch (Throwable throwable) {
			handleException(throwable);
		}
		cleanThreadRelatedStuff();
	}

	/**
	 * 
	 */
	private void cleanThreadRelatedStuff() {
		TaskDetailsThreadLocal.remove();
		details.forEach(ThreadingDetails::clean);
	}

	/**
	 * This method handles the exception scenario.
	 * This method is responsible for throwing exception if configured or not throw if not configured.
	 * Any task can have a exception handler which can handle the exception.
	 * If the exception is handled by exception handler and it is configured to continue post catching, next tasks are continued.
	 * Else Next tasks are not executed.
	 * It throws exception if either we have not attached a exception handler or we have configured task to throw exception even after handling.
	 * @param throwable It throws exception if either we have not attached a exception handler or we have configured task to throw exception even after handling 
	 */
	protected void handleException(Throwable throwable) {
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

	/**
	 * This method configures to throw the exception.
	 * Throwing of exception is handled by Notify Task which is the last task and has responsibility to awake user back.
	 * If we have submitted a request and not called for start, we do not throw the exception, just log the exception.
	 * This method also cleans up all other tasks pending to be executed in the task chain.
	 * @param throwable Throwable which will be thrown
	 * @param shouldThrow If set then Notify task will be configured to throw the exception
	 */
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

	/**
	 * Configure a task to throw exception if occurs even after handling it.
	 * @return Self to continue modification easily
	 */
	public Task shouldThrowExceptionIfOccurs() {
		this.shouldThrowException = true;
		return this;
	}
	
	/**
	 * Configure a task to continue next task if exception handler is present.
	 * @return Self to continue modification easily
	 */
	public Task shouldContinueNextTaskIfExceptionOccurs() {
		this.shouldContinueOnException = true;
		return this;
	}
	
	/**
	 * This method cleans all remaining tasks.
	 */
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

	/**
	 * This method executes next next thread and for doing so calls task executor to call next task.
	 * Also if we can pool the task, we pool it back to save creation time.
	 */
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
	
	/**
	 * Configures if current task be sent back to pool.
	 * @return true if configured to send back to pool
	 */
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
		start(timeToWait, true);
	}
	
	/**
	 * This will start the execution and calling thread will wait for execution.
	 * Throwable is thrown if any task execution throws any exception. The user can also handle the exception by adding onException method call.
	 * If shouldLetExecutingThreadWait is set, the executing thread will also wait. Which means, if a task is also having code which want to create a new chain will make the original task to wait.
	 * This can cause all threads waiting and no work done if new chain is also having same executor in some flow.
	 * If the executor used is having high number of threads and expected to not cause above issue, then we can go ahead with it.
	 * @param timeToWait Max time we will wait for the response.
	 * @param shouldLetExecutingThreadWait Should let an executing thread wait
	 * @throws Throwable Throwable is thrown if any method execution fails
	 */
	public void start(long timeToWait, boolean shouldLetExecutingThreadWait) throws Throwable {
		Task taskInThread = TaskDetailsThreadLocal.get();
		if (shouldLetExecutingThreadWait || Objects.isNull(taskInThread)) {
			NotifyingTask task = new NotifyingTask(uniqueNumber);
			then(task);
			doSubmit();
			pauseExecutingThread(task, timeToWait);
		} else {
			handleSubTask(taskInThread);
		}
	}

	/**
	 * This method handles sub task flow of start where we want to attach requested task chain to currently running chain.
	 * @param taskInThread
	 */
	protected void handleSubTask(Task taskInThread) {
		Task nextTask = taskInThread.nextTask;
		Task thisTask = this;
		if (this instanceof ReturningTask && taskInThread instanceof ReturningTask) {
			thisTask = ((ReturningTask)this).thenConsume(i->((ReturningTask)taskInThread).updateOutput(i));
		}
		thisTask.nextTask = nextTask;
		Task startingTask2 = getStartingTask();
		Task t = startingTask2;
		while (t != null) {
			t.addThreadDetailsFromTask(taskInThread);
			t=t.nextTask;
		}
		taskInThread.nextTask = startingTask2;
	}
	
	/**
	 * This is default start where we are waiting for 5 minutes to complete of a task chain.
	 * @throws Throwable Throwable is thrown if any method execution fails
	 */
	public void start() throws Throwable {
		start(5*60*60);
	}
	
	/**
	 * This is default start where we are waiting for 5 minutes to complete of a task chain.
	 * In this method, if start is called from inside some executing thread, the chain will be included in executing task chain and there will not be wait for this chain to complete.
	 * Control will return directly back, so it is advised to use this method only in case current method is not having any other task depending on this chain.
	 * @throws Throwable Throwable is thrown if any method execution fails
	 */
	public void startWithoutLettingExecutingThreadWait() throws Throwable {
		start(5*60*60, false);
	}
	
	/**
	 * Attaching exception handler. We can have a chain of processes which may be required post handling the exception.
	 * To return back to normal task to which we attached the exception handler please use {@link Task#getParentTask()}. 
	 * @param exceptionHandler Exception handler which will handle the exception
	 * @return Exceutable task, the exception handler task
	 */
	public ExecutableTask onException(Consumer<Throwable> exceptionHandler) {
		RunnableToBeExecuted runnable = ()-> exceptionHandler.accept(this.throwable);
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,runnable, TaskType.NORMAL);
		setExceptionTaskParams(task);
		return task;
	}
	
	/**
	 * Attaching exception handler which will also thought of returning some value which can be processed by next task.
	 * We can have a chain of processes which may be required post handling the exception.
	 * To return back to normal task to which we attached the exception handler please use {@link Task#getParentTask()}.
	 * @param exceptionHandler Exception handler function
	 * @param <T> function return type
	 * @return Returning Task
	 */
	public <T> ReturningTask<T> onExceptionGet(Function<Throwable, T> exceptionHandler) {
		SupplierWhichCanThrowException<T> supplier = ()->exceptionHandler.apply(throwable);
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber, supplier, TaskType.NORMAL);
		setExceptionTaskParams(task);
		return task;
	}
	
	/**
	 * This method add exception handler and also configure to continue post handling the exception.
	 * We can have a chain of processes which may be required post handling the exception.
	 * To return back to normal task to which we attached the exception handler please use {@link Task#getParentTask()}.
	 * @param exceptionHandler Exception handler
	 * @return Executable task
	 */
	public ExecutableTask onExceptionPerfomAndAlsoContinueOtherTasks(Consumer<Throwable> exceptionHandler) {
		RunnableToBeExecuted runnable = ()-> exceptionHandler.accept(this.throwable);
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,runnable, TaskType.NORMAL);
		setExceptionTaskParams(task);
		task.shouldContinueOnException = true;
		return task;
	}
	
	/**
	 * This method add exception handler and also configure to continue post handling the exception.
	 * We can have a chain of processes which may be required post handling the exception.
	 * To return back to normal task to which we attached the exception handler please use {@link Task#getParentTask()}.
	 * @param exceptionHandler Exception handler function
	 * @param <T> type of function return type
	 * @return Returning task
	 */
	public <T> ReturningTask<T> onExceptionGetAndAlsoContinueOtherTasks(Function<Throwable, T> exceptionHandler) {
		SupplierWhichCanThrowException<T> supplier = ()->exceptionHandler.apply(throwable);
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber, supplier, TaskType.NORMAL);
		setExceptionTaskParams(task);
		
		return task;
	}

	/**
	 * This method configures the exception task and current task to maintain the task chains.
	 * @param task Exception task
	 */
	protected void setExceptionTaskParams(Task task) {
		task.parentTask = this;
		this.exceptionTask = task;
		task.taskExecutor = taskExecutor;
	}
	
	
	/**
	 * In case we have started an operation, we need to wait till the operation completes.
	 * The caller thread will wait here for specified time or if task completes.
	 * Notify task is used to notify the current task as monitor.  
	 * @param task Notify task
	 * @param timeToWait Max Time for which we need to wait.
	 * @throws Throwable Throws exception if any exception occured in processing or timed out
	 */
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

	/**
	 * Submit means just add the tasks and forget about the processing. It will be processed in same priority and using same task executor but use do not need to wait for completion of task.
	 */
	public void submit() {
		doSubmit();
	}

	/**
	 * This starts the current task chain.
	 */
	protected void doSubmit() {
		Task task = getStartingTask();
		try {
		taskExecutor.enque(task);
		} catch (Exception e) {
			System.err.println(this);
			e.printStackTrace();
		}
	}

	/**
	 * This method returns the starting task of the chain.
	 * @return Starting task of the chain
	 */
	private Task getStartingTask() {
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
		return task;
	}
	
	/**
	 * This method add a task to the chain.
	 * @param task Task which needs to be added to next chain.
	 * @return the chained task
	 */
	public Task then(Task task) {
		Task lastTask = this;
		while (lastTask.nextTask != null) {
			lastTask = lastTask.nextTask;
		}
		lastTask.nextTask = task;
		task.startingTask = this.startingTask;
		task.uniqueNumber = this.uniqueNumber;
		task.taskExecutor = this.taskExecutor;
		task.parentTask = lastTask.parentTask;
		//setThreadSpecificAttributesToTask(task);
		task.addThreadDetailsFromTask(this);
		return task;
	}

	
	protected void addThreadDetailsFromTask(Task task) {
		for (ThreadingDetails detail : task.getThreadingDetails()) {
			this.addThreadRelatedDetails(detail);
		}
	}
	
	protected void setThreadSpeceficAttributesToTaskFromOther(Task source, Task destination) {
		for (ThreadingDetails detail : source.getThreadingDetails()) {
			destination.addThreadRelatedDetails(detail);
		}
	}
	
	
	
	/**
	 * This method add a new task in chain which will be run post running current task which will be running the supplier and data can be used further as configured in next tasks.
	 * This creates {@link ReturningTask} which can be then 
	 * <ul> <li> used to consume the data by using {@link ReturningTask#thenConsume(Consumer)} or {@link ReturningTask#thenConsumeMultiple(Consumer...)}.</li>
	 * <li> used to further create new task which will work on this data and return something else by using {@link ReturningTask#then(Function)}.</li>
	 *  </ul>
	 * @param supplier Supplier which will be run post the execution of current task
	 * @param <T> Supplier type
	 * @return Returning task
	 */
	public <T> ReturningTask<T> then(SupplierWhichCanThrowException<T> supplier) {
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber, supplier, TaskType.NORMAL);
		then(task);
		return task;
	}
	
	/**
	 * This method creates a new task for the given runnable which will be added to the task chain.
	 * @param runnable Runnable to be executed post the current task
	 * @return Executable task
	 */
	public Task then(RunnableToBeExecuted runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,runnable, TaskType.NORMAL);
		then(task);
		return task;
	}
	
	/**
	 * This method is similar to {@link Task#then(SupplierWhichCanThrowException)} with a difference that new task will be treated as slow task.
	 * @param supplier Supplier to be executed in this new task
	 * @param <T> Supplier type
	 * @return Returning Task
	 */
	public <T> ReturningTask<T> thenSlow(SupplierWhichCanThrowException<T> supplier) {
		ReturningTask<T> task = ReturningTask.getFromPool(uniqueNumber,supplier, TaskType.SLOW);
		then(task);
		return task;
	}
	
	/**
	 * This method is similar to {@link Task#then(RunnableToBeExecuted)} with a difference that new task will be treated as slow task.
	 * @param runnable Runnable to be executed in this new task
	 * @return Executable task
	 */
	public Task thenSlow(RunnableToBeExecuted runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber, runnable, TaskType.SLOW);
		then(task);
		return task;
	}
	
	/**
	 * There can be a scenario where we want items of collection to be consumed in parallel.
	 * This method will create such task which will be added next to current task.
	 * @param tp Task type
	 * @param collection Collection to be worked on
	 * @param consumerToWorkOn Consumer  Consumer of each item in Collection
	 * @param <T> Supplier type
	 * @return ParallelExecuting Task
	 */
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
	
	/**
	 * This method helps for a requirement where we want to work on same data in different consumers in parallel.
	 * Consider you have a data which needs to be added to cache as well as to database etc. 
	 * Or consider you have a data for which you want to update multiple tables or work on different sqls in parallel.
	 * This task will help doing the things concurrently.
	 * This task will be added next to the data.
	 * @param tp Task type
	 * @param itemToWorkOn Item which needs to be worked upon
	 * @param consumersToWorkOn Consumers which will be executed
	 * @param <T> Supplier type
	 * @return Parallel Executing task
	 */
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
	
	/**
	 * There can be a scenario where we want items of collection to be consumed in parallel.
	 * This method will create such task which will be added next to current task.
	 * This new task will run in Async mode.
	 * Returning back to normal chain will need {@link Task#getParentTask()}
	 * @param tp Task type
	 * @param collection Collection to be worked on
	 * @param consumerToWorkOn Consumer  Consumer of each item in Collection
	 * @param <T> Collection and consumer type
	 * @return ParallelExecuting Task
	 */
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
	
	/**
	 * This method helps for a requirement where we want to work on same data in different consumers in parallel.
	 * Consider you have a data which needs to be added to cache as well as to database etc. 
	 * Or consider you have a data for which you want to update multiple tables or work on different sqls in parallel.
	 * This task will help doing the things concurrently.
	 * The data created in this by different consumers can be merged using {@link ParallelReturningTask#join(Consumer)} or {@link ParallelReturningTask#join(Function)}  
	 * This task will be added next to the data.
	 * This new task will run in Async mode.
	 * Returning back to normal chain will need {@link Task#getParentTask()}
	 * @param tp Task type
	 * @param itemToWorkOn Item to be worked on
	 * @param functions Functions to be run on the data in parallel
	 * @param <T> Function return type
	 * @param <K> Type of item to work on
	 * @return Parallel Returning task
	 */
	public <T, K> ParallelReturningTask<T> thenParallelReturning(TaskType tp, K itemToWorkOn, Collection<Function<K, T>> functions) {
		Supplier<T> suppliers[] = new Supplier[functions.size()];
		int i = 0;
		for (Function<K,T> function : functions) {
			suppliers[i++] = ()->function.apply(itemToWorkOn);
		}
		return thenParallel(tp, suppliers);
	}
	
	/**
	 * This method helps for a requirement where we want to work on same data in different consumers in parallel.
	 * Consider you have a data which needs to be added to cache as well as to database etc. 
	 * Or consider you have a data for which you want to update multiple tables or work on different sqls in parallel.
	 * This task will help doing the things concurrently.
	 * The data created in this by different consumers can be merged using {@link ParallelReturningTask#join(Consumer)} or {@link ParallelReturningTask#join(Function)}  
	 * This task will be added next to the data.
	 * @param tp Task type
	 * @param itemToWorkOn Item to be worked on
	 * @param functions Functions to be run on the data in parallel
	 * @param <T> Function return type
	 * @param <K> Type of item to work on
	 * @return Parallel Returning task
	 */
	public <T, K> ParallelReturningTask<T> thenParallelReturningAsync(TaskType tp, K itemToWorkOn, Collection<Function<K, T>> functions) {
		Supplier<T> suppliers[] = new Supplier[functions.size()];
		int i = 0;
		for (Function<K,T> function : functions) {
			suppliers[i++] = ()->function.apply(itemToWorkOn);
		}
		return thenParallelWithoutWait(tp, suppliers);
	}
	
	/**
	 * This method helps for a requirement where we want to work on same data in different consumers in parallel.
	 * Consider you have a data which needs to be added to cache as well as to database etc. 
	 * Or consider you have a data for which you want to update multiple tables or work on different sqls in parallel.
	 * This task will help doing the things concurrently.
	 * This task will be added next to the data.
	 * This new task will run in Async mode.
	 * Returning back to normal chain will need {@link Task#getParentTask()}
	 * @param tp Task type
	 * @param itemToWorkOn Item which needs to be worked upon
	 * @param consumersToWorkOn Consumers which will be executed
	 * @param <T> Type of item to work on
	 * @return Parallel Executing task
	 */
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
	
	/**
	 * This method create a task with runnables to be run in parallel (In ParallelExecutingTask) which will be run separately. 
	 * Execution of current task will just add this task to the executor pool and won't wait for its completion and will start working on its next task.
	 * Other tasks can be added to this task but will not be coupled with original task chain.
	 * To add new task in original task chain, please use {@link Task#getParentTask()}.
	 * @param tp Task type
	 * @param runnables Runnables to be run
	 * @return Parallel Executing task
	 */
	public ParallelExecutingTask thenParallelWithoutWait(TaskType tp, RunnableToBeExecuted... runnables) {
		ParallelExecutingTask parallelExecutingTask = ParallelExecutingTask.getFromPool(uniqueNumber, tp, runnables);
		UnlinkedTask task = new UnlinkedTask(uniqueNumber, tp, parallelExecutingTask);
		includeUnlinkTask(parallelExecutingTask, task);
		return parallelExecutingTask;
	}
	
	/**
	 *  This method create a task with runnables to be run in parallel (In ParallelExecutingTask) which will be run separately. 
	 * Execution of current task will just add this task to the executor pool and won't wait for its completion and will start working on its next task.
	 * Other tasks can be added to this task but will not be coupled with original task chain.
	 * To add new task in original task chain, please use {@link Task#getParentTask()}.
	 * In this method, task executor is also accepted in which this new task will be executed.
	 * @param tp Task type
	 * @param taskExecutor Task executor where this new task will run
	 * @param runnables Runnables
	 * @return Parallel Executing Task
	 */
	public ParallelExecutingTask thenParallelWithoutWait(TaskType tp, TaskExecutor taskExecutor, RunnableToBeExecuted... runnables) {
		ParallelExecutingTask parallelExecutingTask = ParallelExecutingTask.getFromPool(uniqueNumber, tp, runnables);
		UnlinkedTask task = new UnlinkedTask(uniqueNumber, tp, parallelExecutingTask)
				.setTaskExecutorForAsyncTask(taskExecutor);
		includeUnlinkTask(parallelExecutingTask, task);
		parallelExecutingTask.taskExecutor = taskExecutor;
		return parallelExecutingTask;
	}

	/**
	 * Include unlink task.
	 * @param parallelExecutingTask Task which will be run async
	 * @param task The unlink task which will add the parallelExecutingTask in executor.
	 */
	protected void includeUnlinkTask(Task parallelExecutingTask, UnlinkedTask task) {
		then(task);
		parallelExecutingTask.taskExecutor = taskExecutor;
		Task t = task.nextTask;
		while (t != null) {
			t.addThreadDetailsFromTask(this);
		}
	}
	
	/**
	 * * This method create a task with runnables to be run in parallel (In ParallelExecutingTask) which will be run separately. 
	 * Execution of current task will just add this task to the executor pool and won't wait for its completion and will start working on its next task.
	 * Other tasks can be added to this task but will not be coupled with original task chain.
	 * To add new task in original task chain, please use {@link Task#getParentTask()}.
	 * @param runnables Runnables
	 * @return ParallelExecutingTask
	 */
	public ParallelExecutingTask thenParallelWithoutWait(RunnableToBeExecuted... runnables) {
		return thenParallelWithoutWait(TaskType.NORMAL, runnables);
	}
	
	/**
	 * * This method create a task with runnables to be run in parallel (In ParallelExecutingTask) which will be run separately. 
	 * Execution of current task will just add this task to the executor pool and won't wait for its completion and will start working on its next task.
	 * Other tasks can be added to this task but will not be coupled with original task chain.
	 * To add new task in original task chain, please use {@link Task#getParentTask()}.
	 * The task will be a slow task.
	 * @param runnables Runnables
	 * @return Parallel Executing Task
	 */
	public ParallelExecutingTask thenParallelWithoutWaitSlow(RunnableToBeExecuted... runnables) {
		return thenParallelWithoutWait(TaskType.SLOW, runnables);
	}

	/**
	 * This method creates a task which will run multiple runnables in parallel.
	 * Once all task of runnables completes, then only another task which can be added by using methods in {@link ParallelExecutingTask}.
	 * @param runnables Runnables
	 * @return Parallel Executing task
	 */
	public ParallelExecutingTask thenParallel(RunnableToBeExecuted... runnables) {
		return thenParallel(TaskType.NORMAL, runnables);
	}

	/**
	 * This method creates a task which will run multiple runnables in parallel.
	 * Once all task of runnables completes, then only another task which can be added by using methods in {@link ParallelExecutingTask}.
	 * @param tp Task type
	 * @param runnables Runnables
	 * @return Parallel Executing Task
	 */
	public ParallelExecutingTask thenParallel(TaskType tp, RunnableToBeExecuted... runnables) {
		ParallelExecutingTask parallelExecutingTask = ParallelExecutingTask.getFromPool(uniqueNumber, tp, runnables);
		then(parallelExecutingTask);
		return parallelExecutingTask;
	}
	
	/**
	 * This method creates a task which is actually having multiple suppliers. These suppliers will run in parallel
	 * and can be joined by using {@link ParallelReturningTask#join(Consumer)} or {@link ParallelReturningTask#join(Function)}
	 * @param suppliers Suppliers
	 * @param <T> Supplier type
	 * @return Parallel Returning task
	 */
	public <T> ParallelReturningTask<T> thenParallel(Supplier<T>... suppliers) {
		ParallelReturningTask<T> parallelExecutingTask = thenParallel(TaskType.NORMAL, suppliers);
		return parallelExecutingTask;
	}

	/**
	 * This method creates a task which is actually having multiple suppliers. These suppliers will run in parallel
	 * and can be joined by using {@link ParallelReturningTask#join(Consumer)} or {@link ParallelReturningTask#join(Function)}.
	 * @param tp Task type
	 * @param suppliers Suppliers
	 * @param <T> Supplier type
	 * @return ParallelRetruning task
	 */
	public <T> ParallelReturningTask<T> thenParallel(TaskType tp, Supplier<T>... suppliers) {
		ParallelReturningTask<T> parallelExecutingTask = ParallelReturningTask.getFromPool(uniqueNumber,tp,suppliers);
		then(parallelExecutingTask);
		return parallelExecutingTask;
	}
	
	/**
	 * This method creates a task which is actually having multiple suppliers. These suppliers will run in parallel
	 * and can be joined by using {@link ParallelReturningTask#join(Consumer)} or {@link ParallelReturningTask#join(Function)}.
	 * This task runs in async mode. To return back to original task, please use  {@link Task#getParentTask()}.
	 * @param tp Task type
	 * @param suppliers Suppliers
	 * @param <T> Supplier type
	 * @return ParallelRetruning task
	 */
	public <T> ParallelReturningTask<T> thenParallelWithoutWait(TaskType tp, Supplier<T>... suppliers) {
		ParallelReturningTask<T> parallelReturningTask = ParallelReturningTask.getFromPool(uniqueNumber,tp,suppliers);
		UnlinkedTask task = new UnlinkedTask(uniqueNumber, tp, parallelReturningTask);
		includeUnlinkTask(parallelReturningTask, task);
		return parallelReturningTask;
	}
	
	/**
	 * This method creates a task which will run multiple runnables in parallel.
	 * Once all task of runnables completes, then only another task which can be added by using methods in {@link ParallelExecutingTask}.
	 * This will be slow task. All runnables will be treated to be run as slow task.
	 * @param runnables Runnables
	 * @return Parallel Executing task
	 */
	public ParallelExecutingTask thenParallelSlow(RunnableToBeExecuted... runnables) {
		return thenParallel(TaskType.SLOW, runnables);
	}
	
	/**
	 * This method creates a task which is actually having multiple suppliers. These suppliers will run in parallel
	 * and can be joined by using {@link ParallelReturningTask#join(Consumer)} or {@link ParallelReturningTask#join(Function)}.
	 * This will be a slow task and all suppliers will run as slow task. 
	 * @param suppliers Suppliers
	 * @param <T> Supplier type
	 * @return ParallelRetruning task
	 */
	public <T> ParallelReturningTask<T> thenParallelSlow(Supplier<T>... suppliers) {
		return thenParallel(TaskType.SLOW, suppliers);
	}

	/**
	 * This adds task chain a way to have sub task of a task. The parent denotes the parent task of the branched chain.
	 * @return Parent task
	 */
	public Task getParentTask() {
		return this.parentTask;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name + " Task [type: " + this.getClass() + ", uniqueNumber: " + uniqueNumber + ", taskType : " + taskType + "]";
	}

	/**
	 * Get task executor.
	 * @return Task executor for this task
	 */
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}
	
	/**
	 * Get start task from which we need to start.
	 * @return Starting task
	 */
	public Task getStartTask() {
		return this.startingTask;
	}

	/**
	 * Get name of task (for debugging purposes).
	 * @return Name of task
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set name of the task. This helps for diagnostics.
	 * @param name Name of the task
	 * @return Self to ease modification
	 */
	public Task setName(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * Add thread local handling required for this task.
	 * If already present, it will not add again.
	 * @param threadingDetails Threading details
	 * @param <T> type of threading details
	 * @return Self for further use
	 */
	public <T> Task addThreadRelatedDetails(ThreadingDetails<T> threadingDetails) {
		if (!containsProcessingConsumer(threadingDetails.getProcessingForEachThread())) {
			details.add(threadingDetails);
		}
		return this;
	}

	/**
	 * Do we already have a processor in threading details?
	 * @param processingConsumer processing consumer
	 * @return true if contains
	 */
	public boolean containsProcessingConsumer(Consumer processingConsumer) {
		for (ThreadingDetails detail : this.details) {
			if (detail.getProcessingForEachThread() == processingConsumer) {
				return true;
			}
		}
		return false;
	}
	
	protected List<ThreadingDetails> getThreadingDetails() {
		return this.details;
	}
	
}
