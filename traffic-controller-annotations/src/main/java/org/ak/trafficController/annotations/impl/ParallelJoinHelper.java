package org.ak.trafficController.annotations.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;

import org.ak.trafficController.Task;

/**
 * Parallel join helper. It is used in parallel operations.
 * Main use is for managing thread locals and task chains and outputs.
 * @author Amit Khosla
 */
@Named
public class ParallelJoinHelper {
	static ThreadLocal<Task> taskChain = new ThreadLocal<>();
	static ThreadLocal<List<Integer>> parallelChain = new ThreadLocal<>();
	static Map<Integer, Map<Integer, Object>> map = new ConcurrentHashMap();
	final static Object NULL_OBJECT = new Object();
	
	/**
	 * This method adds parallel task id to the list. A list is maintained to identify sub chains.
	 * @param parallelTaskId Parallel task id
	 */
	public static void setParallelTaskId(Integer parallelTaskId) {
		List<Integer> list = parallelChain.get();
		if (Objects.isNull(list)) {
			list = new ArrayList<>();
			parallelChain.set(list);
		}
		list.add(parallelTaskId);
	}
	
	/**
	 * Get parallel id for current request from thread local.
	 * Returns last element added (lowest node of chain).
	 * @return Current parallel Id
	 */
	public static Integer getParallelId() {
		List<Integer> list = parallelChain.get();
		return list.get(list.size() -1);
	}
	
	/**
	 * Remove parallel id.
	 * Removes the parallel id. This should be the last element.
	 * @param parallelId Parallel id to be removed
	 */
	public static void removeParallelId(Integer parallelId) {
		List<Integer> list = parallelChain.get();
		if (Objects.isNull(list)) {
			list.remove(parallelId);
		}
	}
	
	/**
	 * Get object key for parallel task. As parallel tasks will work on their own, this identifier let them store their output in the common map which join will use.
	 * @return Object id for current parallel task
	 */
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
	
	/**
	 * Remove task chain. Used for cleanup.
	 * @param taskChainId Task chain id
	 */
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
	
	/**
	 * Set current task in thread local.
	 * @param task Task mainly parallel task
	 */
	public static void setTask(Task task) {
		taskChain.set(task);
	}
	
	/**
	 * Get current task chain reference.
	 * @return Task, usually parallel task
	 */
	public static Task getTask() {
		return taskChain.get();
	}

	/**
	 * Put result of a task ran in parallel (parallel task id) and having task id (task id).
	 * @param parallelTaskId Parallel task id which parallel task is running
	 * @param taskId Task id whose output has to be stored
	 * @param result Output of the task
	 */
	public static void putObject(int parallelTaskId, int taskId, Object result) {
		map.get(parallelTaskId).put(taskId, result);
	}

	/**
	 * Remove the task from task chain.
	 */
	public static  void removeTask() {
		taskChain.remove();
	}
}
