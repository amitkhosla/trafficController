package org.ak.trafficController;

import org.ak.trafficController.pool.ObjectPoolManager;

public class ExecutableTask extends Task {

	static ExecutableTask getFromPool(int unique, RunnableToBeExecuted runnable, TaskType taskType) {
		ExecutableTask et = ObjectPoolManager.getInstance().getFromPool(ExecutableTask.class, ()->new ExecutableTask(unique, runnable, taskType));
		et.taskType = taskType;
		et.startingTask = et;
		et.runnable = runnable;
		et.uniqueNumber = unique;
		return et;
	}
	
	private RunnableToBeExecuted runnable;

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
