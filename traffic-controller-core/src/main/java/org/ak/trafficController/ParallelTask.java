package org.ak.trafficController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * We want to run things in parallel.
 * Once execution of all parallel tasks is complete then only we want to move ahead.
 * @author Amit Khosla
 *
 */
public abstract class ParallelTask<T> extends Task {

	static Logger logger = Logger.getLogger(ParallelTask.class.getName());
	public ParallelTask(int unique, TaskType taskType) {
		super(unique,taskType);
	}

	protected List<Task> tasks = new ArrayList<Task>(); 
	protected AtomicInteger tasksLeftRef = new AtomicInteger();
	
	protected void addRunnables(Runnable... runnables) {
		for (Runnable runnable : runnables) {
			tasks.add(getExecutable(runnable, TaskType.NORMAL));
		}
	}
	
	protected void addSlowRunnables(Runnable... runnables) {
		for (Runnable runnable : runnables) {
			tasks.add(getExecutable(runnable, TaskType.SLOW));
		}
	}

	protected ExecutableTask getExecutable(Runnable runnable, TaskType taskType) {
		return ExecutableTask.getFromPool(uniqueNumber,()->executeRunnable(runnable), taskType);
	}

	public void executeRunnable(Runnable runnable) {
		runnable.run();
		postTaskRun();
	}
	
	protected void postTaskRun() {
		int tasksLeft = tasksLeftRef.decrementAndGet();
		//TaskExecutorNewTest.logMessages("tasks left ..." + tasksLeft + " for " + uniqueNumber);
		if (tasksLeft == 0) {
			//TaskExecutorNewTest.logMessages("completed parallel ...for " + uniqueNumber);
			this.tasks.clear();
			this.executeNextTask();
		}
	}
	
	@Override
	protected void execute() {
		executeCurrentTask();
	}
	
	@Override
	protected void executeCurrentTask() {
		logger.finest("about to execute current task for unique id" + this.uniqueNumber);
		this.tasksLeftRef.set(this.tasks.size());
		for(Task task:tasks) {
			executeInternalTask(task);
		};
	}

	public void executeInternalTask(Task task) {
		taskExecutor.enque(task);
	}
	
	///TODO - WE CAN ADD WAY TO ADD CHAIN THE SUB TASKS. TWO CLASSES CAN BE CREATED.

	@Override
	public void clean() {
		logger.finest(this + " clean called " + tasks + " clear is being called" + this.uniqueNumber);
		super.clean();
		tasks.clear();
		tasksLeftRef.set(0);
	}
	
}
