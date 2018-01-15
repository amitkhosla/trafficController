package org.ak.trafficController;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.Task.TaskType;
import org.ak.trafficController.messaging.mem.InMemoryQueue;


public class TaskExecutor {
	static Logger LOGGER = Logger.getLogger(TaskExecutor.class.getName());
	static TaskExecutor instance;
	InMemoryQueue<Task> fastChannel = new InMemoryQueue<Task>("fastChannel");
	InMemoryQueue<Task> slowChannel = new InMemoryQueue<Task>("slowChannel");
	
	int numberOfFastQueueConsumers;
	int numberOfSlowQueueConsumers;
	
	public TaskExecutor() {
	}
	
	public void enque(Task nextTask) {
		try{
			switch (nextTask.taskType) {
			case NORMAL:
				fastChannel.add(nextTask);
				break;
			case SLOW :
				slowChannel.add(nextTask);
				break;
			case NOTIFY:
				nextTask.execute();
			}
		} catch (RuntimeException re) {
			LOGGER.log(Level.WARNING, "failed to attach task..." + nextTask, re);
		}
	}
	
	public void init() {
		fastChannel.setDirectConsumer(Task::execute);
		fastChannel.setDirectConsumerCount(numberOfFastQueueConsumers);
		slowChannel.setDirectConsumer(Task::execute);
		slowChannel.setDirectConsumerCount(numberOfSlowQueueConsumers);
	}
	
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

	public ExecutableTask of(Runnable runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(ti.incrementAndGet(),runnable, TaskType.NORMAL);
		task.taskExecutor = this;
		return task;
	}
	
	public <T> ReturningTask<T> of(Supplier<T> supplier) {
		ReturningTask<T> rt =ReturningTask.getFromPool(ti.incrementAndGet(),supplier, TaskType.NORMAL);
		rt.taskExecutor = this;
		return rt;
	}
	
	public ParallelExecutingTask parallelExecutingTasks(Runnable... runnables) {
		ParallelExecutingTask task = ParallelExecutingTask.getFromPool( ti.incrementAndGet(), TaskType.NORMAL, runnables);
		task.taskExecutor = this;
		return task;
	}
	
	public <T> ParallelReturningTask<T> parallelExecutingTasks(Supplier<T>... runnables) {
		ParallelReturningTask<T> task = ParallelReturningTask.getFromPool(ti.incrementAndGet(),TaskType.NORMAL,runnables);
		task.taskExecutor = this;
		return task;
	}
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
	
	protected Integer generateNewUniqueNumber() {
		return this.ti.incrementAndGet();
	}

	public int getNumberOfFastQueueConsumers() {
		return numberOfFastQueueConsumers;
	}

	public TaskExecutor setNumberOfFastQueueConsumers(int numberOfFastQueueConsumers) {
		this.numberOfFastQueueConsumers = numberOfFastQueueConsumers;
		return this;
	}

	public int getNumberOfSlowQueueConsumers() {
		return numberOfSlowQueueConsumers;
	}

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
	public TaskExecutor getTaskExecutorWithConsumersMultipleOfCores(int factorForFastQueuePercent, int factorForSlowQueuePercent) {
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
	public TaskExecutor getTaskExecutorWithDefinedNumberOfConsumers(int fastConsumers, int slowConsumers) {
		TaskExecutor executor = new TaskExecutor();
		executor.setNumberOfFastQueueConsumers(fastConsumers).setNumberOfSlowQueueConsumers(slowConsumers).init();
		return executor;
	}

	private int getConsumersForProcessors(int processors, int factorForFastQueue) {
		int count = processors * factorForFastQueue / 100;
		if (count == 0) {
			count = 1;
		}
		return count;
	}
}
