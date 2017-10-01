package org.ak.trafficController;

import junit.framework.Assert;

import org.ak.trafficController.Task.TaskType;
import org.ak.trafficController.pool.ObjectPoolManager;
import org.junit.Test;


public class NotifyingTaskTest {
	
	@Test
	public void testGetFromPool() {
		NotifyingTask nt = NotifyingTask.getFromPool(3);
		Assert.assertEquals(3, nt.uniqueNumber.intValue());
		Assert.assertEquals(TaskType.NOTIFY, nt.taskType);
	}
	
	@Test
	public void testExecuteCurrentTask() {
		StringBuilder sb = new StringBuilder();
		NotifyingTask nt = new NotifyingTask(234) {
			protected void notifyBack() {
				sb.append("notifyBack called");
			};
		};
		nt.executeCurrentTask();
		Assert.assertEquals("notifyBack called", sb.toString());
	}
}
