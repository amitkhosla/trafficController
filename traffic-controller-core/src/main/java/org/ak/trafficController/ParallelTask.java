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
	/**
	 * Constructor to create parallel task.
	 * @param unique Unique id
	 * @param taskType Tasks type
	 */
	public ParallelTask(int unique, TaskType taskType) {
		super(unique,taskType);
	}

	protected List<Task> tasks = new ArrayList<Task>(); 
	protected AtomicInteger tasksLeftRef = new AtomicInteger();
	
	
	
	/**
	 * Add more tasks using runnables passed and assign these tasks provided executor and set them as passed task type.
	 * @param taskType Task type
	 * @param executor Executor
	 * @param runnables Runnables
	 */
	public void addRunnables(TaskType taskType, TaskExecutor executor, RunnableToBeExecuted... runnables) {
		addRunnables(taskType, executor, null, runnables);
	}
	
	/**
	 * Add more tasks using runnables passed and assign these tasks provided executor and set them as passed task type.
	 * @param taskType Task type
	 * @param executor Executor
	 * @param name Name of the task for diagnostics
	 * @param runnables Runnables
	 */
	public void addRunnables(TaskType taskType, TaskExecutor executor, String name, RunnableToBeExecuted... runnables) {
		for (RunnableToBeExecuted runnable : runnables) {
			ExecutableTask task = getExecutable(runnable, taskType);
			task.taskExecutor = executor;
			task.name = name;
			tasks.add(task);
		}
	}
	
	/**
	 * Add runnable tasks to the parallel task.
	 * @param runnables Runnables
	 */
	public void addRunnables(RunnableToBeExecuted... runnables) {
		for (RunnableToBeExecuted runnable : runnables) {
			tasks.add(getExecutable(runnable, TaskType.NORMAL));
		}
	}
	
	/**
	 * Add runnable tasks to the parallel task. These new tasks will be slow tasks.
	 * @param runnables Runnables
	 */
	public void addSlowRunnables(RunnableToBeExecuted... runnables) {
		for (RunnableToBeExecuted runnable : runnables) {
			tasks.add(getExecutable(runnable, TaskType.SLOW));
		}
	}
	
	/**
	 * Adding a task to be run as one of the parallel tasks in this bunch.
	 * @param task Task to be run as parallel
	 */
	public void addTask(Task task) {
		task.parentTask = this.parentTask;
		task.then(this::postTaskRun);
		tasks.add(task);
	}

	/**
	 * This task helps in creating executable task from runnable.
	 * @param runnable Runnable
	 * @param taskType Task type
	 * @return Executable task which will run a part of this bunch
	 */
	protected ExecutableTask getExecutable(RunnableToBeExecuted runnable, TaskType taskType) {
		ExecutableTask task = ExecutableTask.getFromPool(uniqueNumber,()->executeRunnable(runnable), taskType);
		task.taskExecutor = this.taskExecutor;
		return task;
	}

	/**
	 * This is helper method to make sure that post running runnable cleanup is done to ensure the closure of the task.
	 * @param runnable Runnable
	 * @throws Throwable Throws when runnable thrown any
	 */
	public void executeRunnable(RunnableToBeExecuted runnable) throws Throwable {
		runnable.run();
		postTaskRun();
	}
	
	/**
	 * Simple logic to make bunch work async. 
	 * First we set the task count at time of process start.
	 * Then for each task we reduce the count.
	 * And at the end when the count reduces to 0, we execute next task.
	 */
	protected void postTaskRun() {
		int tasksLeft = tasksLeftRef.decrementAndGet();
		//TaskExecutorNewTest.logMessages("tasks left ..." + tasksLeft + " for " + uniqueNumber);
		if (tasksLeft == 0) {
			//TaskExecutorNewTest.logMessages("completed parallel ...for " + uniqueNumber);

			this.executeNextTask();
		}
	}
	
	@Override
	protected void executeInternal() {
		executeCurrentTask();
	}
	
	/**
	 * We add all tasks to the executor.
	 */
	@Override
	protected void executeCurrentTask() {
		logger.finest("about to execute current task for unique id" + this.uniqueNumber);
		this.tasksLeftRef.set(this.tasks.size());
		for(Task task:tasks) {
			executeInternalTask(task);
		};
	}

	/**
	 * Enqueue the task to its executor.
	 * @param task
	 */
	public void executeInternalTask(Task task) {
		if (task.taskExecutor != null) {
			taskExecutor.enque(task);
		} else {
			taskExecutor.enque(task);
		}
	}
	

	@Override
	public void clean() {
		logger.finest(this + " clean called " + tasks + " clear is being called" + this.uniqueNumber);
		super.clean();
		tasks.clear();
		tasksLeftRef.set(0);
	}
	
}
