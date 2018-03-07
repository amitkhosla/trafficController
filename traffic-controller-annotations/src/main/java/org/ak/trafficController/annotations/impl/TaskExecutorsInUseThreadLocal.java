package org.ak.trafficController.annotations.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Task Executors In use thread local.
 * This keeps record in current thread which all thread locals are in use. 
 * If an executor which is already in use is retried, the task should be executed directly.
 * @author amit.khosla
 *
 */
public class TaskExecutorsInUseThreadLocal {
	private static ThreadLocal<List<String>> taskExecutorsUsed = new ThreadLocal<>();
	
	/**
	 * Set a task executor to thread local.
	 * @param name Name of task executor
	 */
	public static void setTaskExecutor(String name) {
		List<String> list = taskExecutorsUsed.get();
		if (Objects.isNull(list)) {
			list = new ArrayList<>();
			taskExecutorsUsed.set(list);
		}
		list.add(name);
	}
	
	/**
	 * Remove task executor from thread local post completion of task.
	 * @param name Task executor name which needs to be removed
	 */
	public static void removeTaskExecutor(String name) {
		List<String> list = taskExecutorsUsed.get();
		if (!Objects.isNull(list)) {
			list.remove(name);
			if (list.size() == 0) {
				taskExecutorsUsed.remove();
			}
		}
	}
	
	/**
	 * Is task executor present?
	 * @param name Name of task executor to be checked
	 * @return true if task executor already registered for current request
	 */
	public static boolean isTaskExecutorPresent(String name) {
		List<String> list = taskExecutorsUsed.get();
		if (Objects.isNull(list)) {
			return false;
		}
		
		return list.contains(name);
	}
}
