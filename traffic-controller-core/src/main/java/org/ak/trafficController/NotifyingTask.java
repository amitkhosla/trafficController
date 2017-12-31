package org.ak.trafficController;

public class NotifyingTask extends Task {
	
	public NotifyingTask(int unique) {
		super(unique, TaskType.NOTIFY);
	}
	
	@Override
	protected void executeCurrentTask() {
		try {
			Thread.sleep(5l);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		synchronized (this) { 
			this.notifyAll();
		}
	}

	@Override
	public boolean canSendBackToPool() {
		return false;
	}
}
