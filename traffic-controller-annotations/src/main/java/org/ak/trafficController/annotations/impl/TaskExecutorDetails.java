package org.ak.trafficController.annotations.impl;

import org.ak.trafficController.TaskExecutor;

/**
 * A DTO to store task executor details.
 * @author amit.khosla
 *
 */
public class TaskExecutorDetails {
	/**
	 * Task executor.
	 */
	private TaskExecutor taskExecutor;
	/**
	 * Name of task executor.
	 */
	private String name;
	/**
	 * Task executor.
	 * @return Task executor
	 */
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}
	/**
	 * Set task executor.
	 * @param taskExecutor Task executor to be set
	 * @return This object for better use
	 */
	public TaskExecutorDetails setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}
	
	/**
	 * Name of the executor.
	 * @return Name of executor
	 */
	public String getName() {
		return name;
	}
	/**
	 * Set name of executor as passed.
	 * @param name Name of executor
	 * @return This object for further use
	 */
	public TaskExecutorDetails setName(String name) {
		this.name = name;
		return this;
	}
}
