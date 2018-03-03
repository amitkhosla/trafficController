package org.ak.trafficController;

/**
 * This task is responsible for notifying user or throwing exception etc. Caller when run {@link Task#start()} or {@link Task#start(long)} method, this task is added to task chain.
 * Thread calling {@link Task#start()} or {@link Task#start(long)} waits for this this task to send notification.
 * @author amit.khosla
 *
 */
public class NotifyingTask extends Task {
	
	/**
	 * This will help in identifying if it was timed out or successfully completed.
	 */
	boolean executed = false;
	
	public NotifyingTask(int unique) {
		super(unique, TaskType.NOTIFY);
	}
	
	@Override
	protected void executeCurrentTask() {
		try {
			Thread.sleep(5l);//small delay to ensure not having issue of waiting post notification.
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.executed = true;
		synchronized (this) { 
			this.notifyAll();
		}
	}

	@Override
	public boolean canSendBackToPool() {
		return false;
	}
}
