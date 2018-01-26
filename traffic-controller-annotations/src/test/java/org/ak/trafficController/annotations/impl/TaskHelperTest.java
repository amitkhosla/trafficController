package org.ak.trafficController.annotations.impl;

import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;

import org.ak.trafficController.TaskExecutor;
import org.ak.trafficController.annotations.api.Submit;
import org.junit.Test;
import org.mockito.Mockito;

import junit.framework.Assert;

import org.ak.trafficController.annotations.api.Controlled;

public class TaskHelperTest {

	@Test
	public void testGetExecutorByNameHavingIntCommaInt() {
		TaskHelper taskHelper = new TaskHelper();
		TaskExecutor taskExecutor = taskHelper.getExecutorByName("2,3");
		Assert.assertEquals(2, taskExecutor.getNumberOfFastQueueConsumers());
		Assert.assertEquals(3, taskExecutor.getNumberOfSlowQueueConsumers());
		
		TaskExecutor taskExecutor1 = taskHelper.getExecutorByName("2 ,3");
		Assert.assertEquals(2, taskExecutor1.getNumberOfFastQueueConsumers());
		Assert.assertEquals(3, taskExecutor1.getNumberOfSlowQueueConsumers());
		Assert.assertNotSame(taskExecutor, taskExecutor1);
		
		TaskExecutor taskExecutor2 = taskHelper.getExecutorByName("2,3");
		Assert.assertEquals(2, taskExecutor2.getNumberOfFastQueueConsumers());
		Assert.assertEquals(3, taskExecutor2.getNumberOfSlowQueueConsumers());
		Assert.assertSame(taskExecutor, taskExecutor2);
		
		TaskExecutor taskExecutor3 = taskHelper.getExecutorByName("2 ,3");
		Assert.assertEquals(2, taskExecutor3.getNumberOfFastQueueConsumers());
		Assert.assertEquals(3, taskExecutor3.getNumberOfSlowQueueConsumers());
		Assert.assertSame(taskExecutor1, taskExecutor3);
	}
	
	@Test
	public void testGetExecutorByNameHavingIntCommaIntPercentage() {
		TaskHelper taskHelper = new TaskHelper();
		int processors = Runtime.getRuntime().availableProcessors();
		int fast = 2*processors;
		int slow = 4*processors;
		TaskExecutor taskExecutor = taskHelper.getExecutorByName("200%,400%");
		Assert.assertEquals(fast, taskExecutor.getNumberOfFastQueueConsumers());
		Assert.assertEquals(slow, taskExecutor.getNumberOfSlowQueueConsumers());
		
		TaskExecutor taskExecutor1 = taskHelper.getExecutorByName("200% ,400%");
		Assert.assertEquals(fast, taskExecutor1.getNumberOfFastQueueConsumers());
		Assert.assertEquals(slow, taskExecutor1.getNumberOfSlowQueueConsumers());
		Assert.assertNotSame(taskExecutor, taskExecutor1);
		
		TaskExecutor taskExecutor2 = taskHelper.getExecutorByName("200%,400%");
		Assert.assertEquals(fast, taskExecutor2.getNumberOfFastQueueConsumers());
		Assert.assertEquals(slow, taskExecutor2.getNumberOfSlowQueueConsumers());
		Assert.assertSame(taskExecutor, taskExecutor2);
		
		TaskExecutor taskExecutor3 = taskHelper.getExecutorByName("200% ,400%");
		Assert.assertEquals(fast, taskExecutor3.getNumberOfFastQueueConsumers());
		Assert.assertEquals(slow, taskExecutor3.getNumberOfSlowQueueConsumers());
		Assert.assertSame(taskExecutor1, taskExecutor3);
	}
	
	@Test
	public void testGetExecutorByNameHavingIntCommaIntMixPercentage() {
		TaskHelper taskHelper = new TaskHelper();
		int processors = Runtime.getRuntime().availableProcessors();
		int fast = 2*processors;
		int slow = 4*processors;
		TaskExecutor taskExecutor = taskHelper.getExecutorByName("200%,8");
		Assert.assertEquals(fast, taskExecutor.getNumberOfFastQueueConsumers());
		Assert.assertEquals(8, taskExecutor.getNumberOfSlowQueueConsumers());
		
		TaskExecutor taskExecutor1 = taskHelper.getExecutorByName("200% ,8 ");
		Assert.assertEquals(fast, taskExecutor1.getNumberOfFastQueueConsumers());
		Assert.assertEquals(8, taskExecutor1.getNumberOfSlowQueueConsumers());
		Assert.assertNotSame(taskExecutor, taskExecutor1);
		
		TaskExecutor taskExecutor2 = taskHelper.getExecutorByName("20,400%");
		Assert.assertEquals(20, taskExecutor2.getNumberOfFastQueueConsumers());
		Assert.assertEquals(slow, taskExecutor2.getNumberOfSlowQueueConsumers());
		Assert.assertNotSame(taskExecutor, taskExecutor2);
		
		TaskExecutor taskExecutor3 = taskHelper.getExecutorByName("20 ,400%");
		Assert.assertEquals(20, taskExecutor3.getNumberOfFastQueueConsumers());
		Assert.assertEquals(slow, taskExecutor3.getNumberOfSlowQueueConsumers());
		Assert.assertNotSame(taskExecutor1, taskExecutor3);
	}
	
	@Test
	public void testGetExecutorByNameOthers() {
		TaskHelper taskHelper = new TaskHelper();
		TaskExecutor taskExecutor = taskHelper.getExecutorByName("some");
		Assert.assertNull(taskExecutor);
		
		taskExecutor = taskHelper.getExecutorByName("one,some");
		Assert.assertNull(taskExecutor);
		
		taskExecutor = taskHelper.getExecutorByName("2,some");
		Assert.assertNull(taskExecutor);
		
		taskExecutor = taskHelper.getExecutorByName("234a,2");
		Assert.assertNull(taskExecutor);
		
		taskExecutor = taskHelper.getExecutorByName("2,3,4");
		Assert.assertNull(taskExecutor);
		

		Assert.assertEquals(0, taskHelper.taskExecutors.size());
		TaskExecutor mock = Mockito.mock(TaskExecutor.class);
		taskHelper.taskExecutors.put("myString", mock);
		taskExecutor = taskHelper.getExecutorByName("myString");
		Assert.assertEquals(mock, taskExecutor);
	}

}
