package org.ak.trafficController;

/**
 * Thread local to keep track if in start we are starting from some thread which is already in pool.
 * This implies it is not a call by user but an internal call from some system.
 * Thus this should be part of the system. 
 * @author amit.khosla
 */
public class TaskDetailsThreadLocal {
	private static ThreadLocal<Task> taskInCurrentThread = new ThreadLocal<>();
	
	public static void setTask(Task task) {
		taskInCurrentThread.set(task);
	}
	
	public static void remove() {
		taskInCurrentThread.remove();
	}
	
	public static Task get() {
		return taskInCurrentThread.get();
	}
}
