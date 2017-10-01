package org.ak.trafficController;

import org.ak.trafficController.pool.ObjectPoolManager;


public class NotifyingTask extends Task {
	
	static NotifyingTask getFromPool(int unique) {
		NotifyingTask et = ObjectPoolManager.getInstance().getFromPool(NotifyingTask.class, ()->new NotifyingTask(unique));
		et.uniqueNumber = unique;
		return et;
	}
	
	public NotifyingTask(int unique) {
		super(unique, TaskType.NOTIFY);
	}
	
	@Override
	protected void executeCurrentTask() {
		notifyBack();
	}
}
