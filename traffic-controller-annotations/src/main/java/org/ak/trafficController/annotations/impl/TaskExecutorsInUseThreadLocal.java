package org.ak.trafficController.annotations.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TaskExecutorsInUseThreadLocal {
	private static ThreadLocal<List<String>> taskExecutorsUsed = new ThreadLocal<>();
	
	public static void setTaskExecutor(String name) {
		List<String> list = taskExecutorsUsed.get();
		if (Objects.isNull(list)) {
			list = new ArrayList<>();
			taskExecutorsUsed.set(list);
		}
		list.add(name);
	}
	
	public static void removeTaskExecutor(String name) {
		List<String> list = taskExecutorsUsed.get();
		if (!Objects.isNull(list)) {
			list.remove(name);
			if (list.size() == 0) {
				taskExecutorsUsed.remove();
			}
		}
	}
	
	public static boolean isTaskExecutorPresent(String name) {
		List<String> list = taskExecutorsUsed.get();
		if (Objects.isNull(list)) {
			return false;
		}
		
		return list.contains(name);
	}
}
