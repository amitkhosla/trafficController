package org.ak.trafficController.annotations.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;

import org.ak.trafficController.Task;

/**
 * Parallel join helper
 * @author Amit Khosla
 */
@Named
public class ParallelJoinHelper {
	static ThreadLocal<Task> taskChain = new ThreadLocal<>();
	static ThreadLocal<List<Integer>> parallelChain = new ThreadLocal<>();
	static Map<Integer, Map<Integer, Object>> map = new ConcurrentHashMap();
	final static Object NULL_OBJECT = new Object();
	
	public static void setParallelTaskId(Integer parallelTaskId) {
		List<Integer> list = parallelChain.get();
		if (Objects.isNull(list)) {
			list = new ArrayList<>();
			parallelChain.set(list);
		}
		list.add(parallelTaskId);
	}
	
	public static Integer getParallelId() {
		List<Integer> list = parallelChain.get();
		return list.get(list.size() -1);
	}
	
	public static Integer getObjectKeyForParalleldTask() {
		List<Integer> list = parallelChain.get();
		Integer paralleTaskId = list.get(list.size() -1);
		Map<Integer, Object> valueMap = map.get(paralleTaskId);
		if (valueMap == null) {
			valueMap = new ConcurrentHashMap<>();
			valueMap.put(0, NULL_OBJECT);
			map.put(paralleTaskId, valueMap);
			return 0;
		}
		int i = 1;
		while(valueMap.get(i) != null) {
			i++;
		}
		valueMap.put(i, NULL_OBJECT);
		return i;
	}
	
	public static void removeTaskExcutor(Integer taskChainId) {
		List<Integer> list = parallelChain.get();
		//if (!Objects.isNull(list)) {
			int index = list.size()-1;
			Integer lastTaskId = list.get(index);
			if (lastTaskId.equals(taskChainId)) {
				list.remove(index);
			}
			if (list.size() == 0) {
				taskChain.remove();
				parallelChain.remove();
			}
		//}
	}
	
	public static void setTask(Task task) {
		taskChain.set(task);
	}
	
	public static Task getTask() {
		return taskChain.get();
	}

	public static void putObject(int parallelTaskId, int taskId, Object result) {
		map.get(parallelTaskId).put(taskId, result);
	}

	public static  void removeTask() {
		taskChain.remove();
	}
}
