package org.ak.trafficController;

import org.ak.trafficController.Task.TaskType;
import org.ak.trafficController.pool.ObjectPoolManager;
import org.junit.Assert;
import org.junit.Test;


public class ParallelExecutingTaskTest {

	@Test
	public void testGetFromPool() {
		ParallelExecutingTask task = ParallelExecutingTask.getFromPool(1234, TaskType.NORMAL, ()->{}, ()->{});
		Assert.assertEquals(1234, task.uniqueNumber.intValue());
		Assert.assertEquals(2, task.tasks.size());
		for (Object t : task.tasks) {
			Assert.assertEquals(TaskType.NORMAL, ((Task)t).taskType);
		}
	}
}
