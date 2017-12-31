package org.ak.trafficController;

import junit.framework.Assert;

import org.ak.trafficController.Task.TaskType;
import org.ak.trafficController.pool.ObjectPoolManager;
import org.junit.Test;


public class NotifyingTaskTest {
	
	@Test
	public void testGetFromPool() {
		NotifyingTask nt = new NotifyingTask(3);
		Assert.assertEquals(3, nt.uniqueNumber.intValue());
		Assert.assertEquals(TaskType.NOTIFY, nt.taskType);
	}
	
}
