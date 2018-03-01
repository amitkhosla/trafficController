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
	
	
	
	public void addRunnables(TaskType taskType, TaskExecutor executor, RunnableToBeExecuted... runnables) {
		addRunnables(taskType, executor, null, runnables);
	}
	
	public void addRunnables(TaskType taskType, TaskExecutor executor, String name, RunnableToBeExecuted... runnables) {
		for (RunnableToBeExecuted runnable : runnables) {
			ExecutableTask task = getExecutable(runnable, taskType);
			task.taskExecutor = executor;
			task.name = name;
			tasks.add(task);
		}
	}
	
	public void addRunnables(RunnableToBeExecuted... runnables) {
		for (RunnableToBeExecuted runnable : runnables) {
			tasks.add(getExecutable(runnable, TaskType.NORMAL));
		}
	}
	
	public void addSlowRunnables(RunnableToBeExecuted... runnables) {
		for (RunnableToBeExecuted runnable : runnables) {
			tasks.add(getExecutable(runnable, TaskType.SLOW));
		}
	}
	
	public void addTask(Task task) {
		task.parentTask = this.parentTask;
		task.then(this::postTaskRun);
		tasks.add(task);
	}

	protected ExecutableTask getExecutable(RunnableToBeExecuted runnable, TaskType taskType) {
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,()->executeRunnable(runnable), taskType);
		task.taskExecutor = this.taskExecutor;
		return task;
	}

	public void executeRunnable(RunnableToBeExecuted runnable) throws Throwable {
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
	protected void executeInternal() {
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
		if (task.taskExecutor != null) {
			taskExecutor.enque(task);
		} else {
			taskExecutor.enque(task);
		}
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
