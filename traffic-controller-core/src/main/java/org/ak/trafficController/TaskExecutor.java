package org.ak.trafficController;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ak.trafficController.Task.TaskType;
import org.ak.trafficController.messaging.mem.InMemoryQueue;


public class TaskExecutor {
	static Logger LOGGER = Logger.getLogger(TaskExecutor.class.getName());
	static TaskExecutor instance = new TaskExecutor();
	InMemoryQueue<Task> fastChannel = new InMemoryQueue<Task>("fastChannel");
	InMemoryQueue<Task> slowChannel = new InMemoryQueue<Task>("slowChannel");
	
	public TaskExecutor() {
		init();
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
		int processors = Runtime.getRuntime().availableProcessors();
		int half = processors/2;
		if (half == 0) {
			half = 1;
		}
		fastChannel.setDirectConsumer(Task::execute);
		fastChannel.setDirectConsumerCount(processors);
		slowChannel.setDirectConsumer(Task::execute);
		slowChannel.setDirectConsumerCount(half);
		
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
		return instance;
	}
	
	protected Integer generateNewUniqueNumber() {
		return this.ti.incrementAndGet();
	}

}
