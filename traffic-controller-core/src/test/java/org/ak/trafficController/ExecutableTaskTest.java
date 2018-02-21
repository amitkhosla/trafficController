package org.ak.trafficController;

import java.util.concurrent.atomic.AtomicInteger;

import org.ak.trafficController.Task.TaskType;
import org.junit.Assert;
import org.junit.Test;

public class ExecutableTaskTest {

	
	@Test
	public void testExecutableTask() throws Throwable {
		AtomicInteger ai = new AtomicInteger();
		RunnableToBeExecuted runnable = ()->{ai.incrementAndGet();};
		ExecutableTask et = ExecutableTask.getFromPool(234, runnable, TaskType.NORMAL);
		Assert.assertTrue(et.taskType == TaskType.NORMAL);
		Assert.assertEquals(234,et.uniqueNumber.intValue());
		Assert.assertFalse(et.canSendBackToPool());
		
		Assert.assertEquals(0, ai.get());
		et.executeCurrentTask();
		Assert.assertEquals(1, ai.get());
	}
}
