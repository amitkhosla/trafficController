package org.ak.trafficController;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.Task.TaskType;
import org.ak.trafficController.messaging.mem.InMemoryQueue;


/**
 * Task Executor is the class which executes the tasks.
 * We can have multiple task executors.
 * It is advised to use TaskExecutor.getInstance() to make sure we do not end up creating too many executors.
 * @author Amit.Khosla
 */
public class TaskExecutor {
	static Logger LOGGER = Logger.getLogger(TaskExecutor.class.getName());
	static TaskExecutor instance;
	/**
	 * Fast queue where fast tasks will run.
	 */
	InMemoryQueue<Task> fastQueue = new InMemoryQueue<Task>("fastChannel");
	/**
	 * Slow queue where slow tasks will run.
	 */
	InMemoryQueue<Task> slowQueue = new InMemoryQueue<Task>("slowChannel");
	
	int numberOfFastQueueConsumers;
	int numberOfSlowQueueConsumers;
	
	/**
	 * Enqueue a task in the executor or execute notify tasks.
	 * @param nextTask Task which in being enqueued
	 */
	public void enque(Task nextTask) {
		try{
			switch (nextTask.taskType) {
			case NORMAL:
				fastQueue.add(nextTask);
				break;
			case SLOW :
				slowQueue.add(nextTask);
				break;
			case NOTIFY:
				nextTask.execute();
			}
		} catch (RuntimeException re) {
			LOGGER.log(Level.WARNING, "failed to attach task..." + nextTask, re);
		}
	}
	
	/**
	 * Initialize task executor.
	 */
	public void init() {
		fastQueue.setDirectConsumer(Task::execute);
		fastQueue.setDirectConsumerCount(numberOfFastQueueConsumers);
		slowQueue.setDirectConsumer(Task::execute);
		slowQueue.setDirectConsumerCount(numberOfSlowQueueConsumers);
	}
	
	/**
	 * Default settings where fast threads are set to half of processors and slow as same as number of processors.
	 */
	protected void setDefaultSettings() {
		int processors = Runtime.getRuntime().availableProcessors();
		int half = processors/2;
		if (half == 0) {
			half = 1;
		}
		this.numberOfFastQueueConsumers = half;
		this.numberOfSlowQueueConsumers = processors;
	}
	
	AtomicInteger ti = new AtomicInteger();

	/**
	 * Way to get a task and start playing around task chain.
	 * @param runnable Runnable which will be starting point of chain
	 * @return The starting task
	 */
	public ExecutableTask of(RunnableToBeExecuted runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(ti.incrementAndGet(),runnable, TaskType.NORMAL);
		task.taskExecutor = this;
		return task;
	}
	
	/**
	 * This is similar to {@link}{@link TaskExecutor#of(RunnableToBeExecuted)}} with just one difference that task created here will be treated as slow task and will be run by slow tasks handler.
	 * @param runnable Runnable which will be starting point of chain
	 * @return Starting task
	 */
	public ExecutableTask slowOf(RunnableToBeExecuted runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(ti.incrementAndGet(),runnable, TaskType.NORMAL);
		task.taskExecutor = this;
		task.taskType = TaskType.SLOW;
		return task;
	}
	
	/**
	 * Create task which will return some value which will be consumed by next task (if configured by calling @link {@link ReturningTask#thenConsume(java.util.function.Consumer)}).
	 * @param supplier Supplier which will be run
	 * @param <T> type of supplier
	 * @return Returning task
	 */
	public <T> ReturningTask<T> of(SupplierWhichCanThrowException<T> supplier) {
		ReturningTask<T> rt =ReturningTask.getFromPool(ti.incrementAndGet(),supplier, TaskType.NORMAL);
		rt.taskExecutor = this;
		return rt;
	}
	
	/**
	 * Create a task which will execute different runnables in parallel.
	 * @param runnables Executables which we want to run
	 * @return ParallelExecuting task
	 */
	public ParallelExecutingTask parallelExecutingTasks(RunnableToBeExecuted... runnables) {
		ParallelExecutingTask task = ParallelExecutingTask.getFromPool( ti.incrementAndGet(), TaskType.NORMAL, runnables);
		task.taskExecutor = this;
		return task;
	}
	
	/**
	 * Creates a task which will run different suppliers in parallel.
	 * Result can be joined by next task using {@link ParallelReturningTask#join(java.util.function.Consumer)} which will create next task as joiner - consumer of list created by this task
	 * or {@link ParallelReturningTask#join(java.util.function.Function)} which will create next task as joiner - function to retrieve data from list is set as next {@link ReturningTask}.
	 * @param runnables Suppliers which needs to be run
	 * @param <T> type of suppliers
	 * @return ParallelExecuting task
	 */
	public <T> ParallelReturningTask<T> parallelExecutingTasks(Supplier<T>... runnables) {
		ParallelReturningTask<T> task = ParallelReturningTask.getFromPool(ti.incrementAndGet(),TaskType.NORMAL,runnables);
		task.taskExecutor = this;
		return task;
	}
	
	/**
	 * Default task executor. This is default implementation so as to make it easy to use. 
	 * This task executor has 50% of cores as fast task runners and 100% of cores as slow task runner.
	 * e.g., if we have 4 core machine, we will have 2 fast task runners and 4 slow task runners.
	 * @return Default task executor with 50% of cores as fast task runners and 100% of cores as slow task runner
	 */
	public static TaskExecutor getInstance() {
		if (instance == null) {
			synchronized (TaskExecutor.class) {
				if (instance == null) {
					instance = new TaskExecutor();
					instance.setDefaultSettings();
					instance.init();
				}
			}
		}
		return instance;
	}
	
	/**
	 * This method is internal to framework which creates unique number for a given request.
	 * @return New unique number
	 */
	protected Integer generateNewUniqueNumber() {
		return this.ti.incrementAndGet();
	}

	/**
	 * Number of fast task runners.
	 * @return Fast task queue consumer count
	 */
	public int getNumberOfFastQueueConsumers() {
		return numberOfFastQueueConsumers;
	}

	/**
	 * This method set the number of fast task runners.
	 * @param numberOfFastQueueConsumers Number of fast task runners
	 * @return Self so as to carry on any update
	 */
	public TaskExecutor setNumberOfFastQueueConsumers(int numberOfFastQueueConsumers) {
		this.numberOfFastQueueConsumers = numberOfFastQueueConsumers;
		return this;
	}

	/**
	 * Get number of slow task runners.
	 * @return Slow task queue consumer count
	 */
	public int getNumberOfSlowQueueConsumers() {
		return numberOfSlowQueueConsumers;
	}

	/**
	 * This method set the number of slow task runners.
	 * @param numberOfSlowQueueConsumers Number of slow task runners
	 * @return Self so as to carry on any update
	 */
	public TaskExecutor setNumberOfSlowQueueConsumers(int numberOfSlowQueueConsumers) {
		this.numberOfSlowQueueConsumers = numberOfSlowQueueConsumers;
		return this;
	}

	
	/**
	 * This allows to create executor with number of fast and slow consumers as per percent of number of cores.
	 * For example if we have this as fast as 100 and slow as 200, for a system having 4 cores, it will imply 4 consumers of fast thread and 8 threads of slow.
	 * @param factorForFastQueuePercent Fast thread percentage
	 * @param factorForSlowQueuePercent Slow thread percentage
	 * @return TaskExecutor
	 */
	public static TaskExecutor getTaskExecutorWithConsumersMultipleOfCores(int factorForFastQueuePercent, int factorForSlowQueuePercent) {
		TaskExecutor executor = new TaskExecutor();
		int processors = Runtime.getRuntime().availableProcessors();
		executor.setNumberOfFastQueueConsumers(getConsumersForProcessors(processors, factorForFastQueuePercent));
		executor.setNumberOfSlowQueueConsumers(getConsumersForProcessors(processors, factorForSlowQueuePercent));
		executor.init();
		return executor;
	}
	
	/**
	 * Get Task Executor with defined number of consumers.
	 * @param fastConsumers Number of fast consumer
	 * @param slowConsumers Number of slow consumer
	 * @return TaskExecutor
	 */
	public static TaskExecutor getTaskExecutorWithDefinedNumberOfConsumers(int fastConsumers, int slowConsumers) {
		TaskExecutor executor = new TaskExecutor();
		executor.setNumberOfFastQueueConsumers(fastConsumers).setNumberOfSlowQueueConsumers(slowConsumers).init();
		return executor;
	}

	/**
	 * This method helps to calculate number of consumers for given processors.
	 * To calculate it just take given percentage of cores.
	 * e.g., for 4 processors and 50 factor in percentage will return 2.  
	 * @param processors Cores present in system
	 * @param factorInPercentage as percentage
	 * @return Number of consumers
	 */
	protected static int getConsumersForProcessors(int processors, int factorInPercentage) {
		int count = processors * factorInPercentage / 100;
		if (count == 0) {
			count = 1;
		}
		return count;
	}
	
	/**
	 * This method should be used to gracefully shutdown the system.
	 * Please note this method should be called only if we do not need the task executor anymore.
	 */
	public void shutdown() {
		this.fastQueue.shutdown();
		this.slowQueue.shutdown();
	}
}
