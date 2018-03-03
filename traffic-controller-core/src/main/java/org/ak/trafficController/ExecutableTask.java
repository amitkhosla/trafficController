package org.ak.trafficController;

import org.ak.trafficController.pool.ObjectPoolManager;

public class ExecutableTask extends Task {

	/**
	 * Get Executable task from pool and set different attributes.
	 * @param unique UniqueId
	 * @param runnable Runnable to be run
	 * @param taskType Task type
	 * @return Executable task
	 */
	static ExecutableTask getFromPool(int unique, RunnableToBeExecuted runnable, TaskType taskType) {
		ExecutableTask et = ObjectPoolManager.getInstance().getFromPool(ExecutableTask.class, ()->new ExecutableTask(unique, runnable, taskType));
		et.taskType = taskType;
		et.startingTask = et;
		et.runnable = runnable;
		et.uniqueNumber = unique;
		return et;
	}
	
	/**
	 * Runnable which will be executed as part of this task.
	 */
	private RunnableToBeExecuted runnable;

	/**
	 * Constructor to create the executable task.
	 * @param unique Unique if
	 * @param runnable Runnable
	 * @param taskType Task type
	 */
	public ExecutableTask(int unique, RunnableToBeExecuted runnable, TaskType taskType) {
		super(unique, taskType);
		this.runnable  = runnable;
	}
	
	@Override
	public boolean canSendBackToPool() {
		return false;
	}

	@Override
	protected void executeCurrentTask() throws Throwable {
		runnable.run();
	}
	
}
