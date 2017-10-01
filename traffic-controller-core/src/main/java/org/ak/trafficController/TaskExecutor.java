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
	static InMemoryQueue<Task> fastChannel = new InMemoryQueue<Task>("fastChannel");
	static InMemoryQueue<Task> slowChannel = new InMemoryQueue<Task>("slowChannel");
	static {
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
	private static void init() {
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
	
	static AtomicInteger ti = new AtomicInteger();

	public static ExecutableTask of(Runnable runnable) {
		ExecutableTask task = ExecutableTask.getFromPool(ti.incrementAndGet(),runnable, TaskType.NORMAL);
		return task;
	}
	
	public static <T> ReturningTask<T> of(Supplier<T> supplier) {
		ReturningTask<T> rt =ReturningTask.getFromPool( ti.incrementAndGet(),supplier, TaskType.NORMAL);
		return rt;
	}
	
	public static ParallelExecutingTask parallelExecutingTasks(Runnable... runnables) {
		return ParallelExecutingTask.getFromPool( ti.incrementAndGet(), TaskType.NORMAL, runnables);
	}
	
	public static <T> ParallelReturningTask<T> parallelExecutingTasks(Supplier<T>... runnables) {
		return ParallelReturningTask.getFromPool(ti.incrementAndGet(),TaskType.NORMAL,runnables);
	}
	public static TaskExecutor getInstance() {
		// TODO Auto-generated method stub
		return instance;
	}
}
