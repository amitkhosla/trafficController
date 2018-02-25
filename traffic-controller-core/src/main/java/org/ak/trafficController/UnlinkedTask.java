package org.ak.trafficController;

/**
 * This task will create unlinked task. 
 * This means when executed, this task chain will be added directly executor
 * and parent will not be waited.
 * Link with parent is just to get the start point for this task.
 * Once started, this will be a brand new task with its own chain.
 * Parent task will remain available for ease of use but won't be ever executed.
 * This task will act as a linker task. It will link and go.
 * 
 * NOTE : This is a tricky task as it is opening a new chain. You should ideally run submit or start on parent task.
 * @author Amit Khosla
 */
public class UnlinkedTask extends Task {
	
	protected Task taskToBeAsynced;
	
	public UnlinkedTask(int unique, TaskType taskType, Task taskToBeAsynced) {
		super(unique, taskType);
		this.taskToBeAsynced = taskToBeAsynced;
		taskToBeAsynced.parentTask = this;
	}
	
	public UnlinkedTask setTaskExecutorForAsyncTask(TaskExecutor taskExecutor) {
		this.taskToBeAsynced.taskExecutor = taskExecutor;
		return this;
	}

	@Override
	protected void executeCurrentTask() {
		if (taskToBeAsynced.taskExecutor == null) {
			taskExecutor.enque(taskToBeAsynced);
		} else {
			taskToBeAsynced.taskExecutor.enque(taskToBeAsynced);
		}
	}
}
