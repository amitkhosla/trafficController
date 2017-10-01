package org.ak.trafficController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.ak.trafficController.Task.TaskType;
import org.ak.trafficController.pool.ObjectPoolManager;
import org.junit.Assert;
import org.junit.Test;

public class ParallelReturningTaskTest  {

	@Test
	public void testGetFromPool() {
		ParallelReturningTask task = ParallelReturningTask.getFromPool(1234, TaskType.NORMAL, ()->2, ()->4);
		Assert.assertEquals(1234, task.uniqueNumber.intValue());
		Assert.assertEquals(2, task.tasks.size());
		for (Object t : task.tasks) {
			Assert.assertEquals(TaskType.NORMAL, ((Task)t).taskType);
		}
	}
}
