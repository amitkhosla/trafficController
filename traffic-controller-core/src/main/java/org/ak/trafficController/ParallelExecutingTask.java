package org.ak.trafficController;

import org.ak.trafficController.pool.ObjectPoolManager;


public class ParallelExecutingTask<T> extends ParallelTask<T> {

	/**
	 * Get parallel executing task from pool and initialize it on basis of the params
	 * @param unique Unique id
	 * @param taskType Task type
	 * @param runnables Runnables
	 * @return ParallelExecuting task
	 */
	static ParallelExecutingTask getFromPool(int unique, TaskType taskType, RunnableToBeExecuted... runnables) {
		ParallelExecutingTask et = ObjectPoolManager.getInstance().getFromPool(ParallelExecutingTask.class, ()->new ParallelExecutingTask(unique, taskType, runnables));
		et.tasks.clear();
		et.taskType = taskType;
		et.uniqueNumber = unique;
		if (taskType == TaskType.NORMAL) et.addRunnables(runnables);
		if (taskType == TaskType.SLOW) et.addSlowRunnables(runnables);
		et.startingTask = et;
		return et;
	}
	
	/**
	 * Constructor to create parallel executing task.
	 * @param unique Unique id
	 * @param taskType Task type
	 * @param runnables Runnables to be run
	 */
	public ParallelExecutingTask(int unique, TaskType taskType, RunnableToBeExecuted... runnables) {
		super(unique, taskType);
		addRunnables(runnables);
	}
	
	/**
	 * This method allows to add more runnables in the current task.
	 * @param runnables Runnables
	 * @return Same object to easily work further on it.
	 */
	public ParallelExecutingTask and(RunnableToBeExecuted... runnables) {
		addRunnables(runnables);
		return this;
	}
}
