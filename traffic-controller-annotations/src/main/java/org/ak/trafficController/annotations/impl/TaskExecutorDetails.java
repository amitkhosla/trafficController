package org.ak.trafficController.annotations.impl;

import org.ak.trafficController.TaskExecutor;

public class TaskExecutorDetails {
	private TaskExecutor taskExecutor;
	private String name;
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}
	public TaskExecutorDetails setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}
	public String getName() {
		return name;
	}
	public TaskExecutorDetails setName(String name) {
		this.name = name;
		return this;
	}
}
