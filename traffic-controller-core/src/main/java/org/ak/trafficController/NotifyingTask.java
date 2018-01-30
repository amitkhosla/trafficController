package org.ak.trafficController;

public class NotifyingTask extends Task {
	
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
