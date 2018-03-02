package org.ak.trafficController;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.ak.trafficController.Task.TaskType;
import org.junit.Test;

import junit.framework.Assert;


public class TaskTest {

	@Test
	public void testClean() {
		Task task = new Task(234,TaskType.NORMAL) {
			
			@Override
			protected void executeCurrentTask() {
				
			}
		};
		assertEquals(TaskType.NORMAL, task.taskType);
		assertNotNull(task.startingTask);
		assertEquals(234, task.uniqueNumber.intValue());
		
		task.clean();
		assertNull(task.startingTask);
		assertNull(task.uniqueNumber);
		assertNull(task.taskType);
	}
	
	@Test
	public void testExecute() {
		StringBuilder sb = new StringBuilder();
		Task t = new Task(234,TaskType.NORMAL) {
			@Override
			protected void executeCurrentTask() {
				sb.append("execute current task called");
			};
			@Override
			protected void executeNextTask() {
				sb.append("execute next task called");
			}
		};
		t.execute();
		assertTrue(sb.indexOf("execute current task called")>-1);
		assertTrue(sb.indexOf("execute next task called")>-1);
	}
	
	
	@Test
	public void testthenParallelAsyncSingleItemMultipleConsumers() {
		AtomicInteger ai1 = new AtomicInteger();
		AtomicInteger ai2 = new AtomicInteger();
		AtomicInteger ai3 = new AtomicInteger();
		AtomicInteger ai4 = new AtomicInteger();
		AtomicInteger ai5 = new AtomicInteger();
		
		Collection<Consumer<Integer>> consumers = new ArrayList<>();
		consumers.add(i->{ai2.addAndGet(i);});
		consumers.add(i->{ai3.addAndGet(i);});
		consumers.add(i->{ai4.addAndGet(i);});
		consumers.add(i->{ai5.addAndGet(i);});
		
		Task t = TaskExecutor.getInstance().of(()->{})
				.thenParallelAsync(TaskType.NORMAL, 2, consumers );
		
		Assert.assertEquals(TaskType.NORMAL, t.taskType);
		try {
			t.start(100);
		} catch (Throwable e) {
			fail("should not have thrown exception");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertEquals(2, ai2.get());
		Assert.assertEquals(2, ai3.get());
		Assert.assertEquals(2, ai4.get());
		Assert.assertEquals(2, ai5.get());
	}
	
	@Test
	public void testthenParallelAsyncCollectionWithSingleConsumer() {
		List<Integer> list = new ArrayList<>();
		for (int i=1;i<=100;i++) {
			list.add(i);
		}
		
		AtomicInteger a = new AtomicInteger();
		
		try {
			TaskExecutor.getInstance().of(()->{}).thenParallelAsync(TaskType.NORMAL, list, i->{a.addAndGet(i);})
				.start(100);
		} catch (Throwable e) {
			fail("should not have thrown exception");
			e.printStackTrace();
		}
		
		Assert.assertEquals(50*101, a.get());
	}
	
	@Test
	public void testthenParallelSingleItemMultipleConsumers() {
		AtomicInteger ai1 = new AtomicInteger();
		AtomicInteger ai2 = new AtomicInteger();
		AtomicInteger ai3 = new AtomicInteger();
		AtomicInteger ai4 = new AtomicInteger();
		AtomicInteger ai5 = new AtomicInteger();
		
		Collection<Consumer<Integer>> consumers = new ArrayList<>();
		consumers.add(i->{ai2.addAndGet(i);});
		consumers.add(i->{ai3.addAndGet(i);});
		consumers.add(i->{ai4.addAndGet(i);});
		consumers.add(i->{ai5.addAndGet(i);});
		
		Task t = TaskExecutor.getInstance().of(()->{})
				.thenParallel(TaskType.NORMAL, 2, consumers );
		
		Assert.assertEquals(TaskType.NORMAL, t.taskType);
		try {
			t.start(100);
		} catch (Throwable e) {
			fail("should not have thrown exception");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertEquals(2, ai2.get());
		Assert.assertEquals(2, ai3.get());
		Assert.assertEquals(2, ai4.get());
		Assert.assertEquals(2, ai5.get());
	}
	
	@Test
	public void testthenParallelCollectionWithSingleConsumer() {
		List<Integer> list = new ArrayList<>();
		for (int i=1;i<=100;i++) {
			list.add(i);
		}
		
		AtomicInteger a = new AtomicInteger();
		
		try {
			TaskExecutor.getInstance().of(()->{}).thenParallel(TaskType.NORMAL, list, i->{a.addAndGet(i);})
				.start(100);
		} catch (Throwable e) {
			fail("should not have thrown exception");
			e.printStackTrace();
		}
		
		Assert.assertEquals(50*101, a.get());
	}
	
	@Test
	public void testhandleException() {
		AtomicInteger ai = new AtomicInteger(0);
		
		try {
			TaskExecutor.getInstance().of(()->{throw new Exception("MyException");})
				.shouldContinueNextTaskIfExceptionOccurs()
				.then(()->{ai.incrementAndGet();}).start(10);
			//fail ("should not throw exception");
		} catch (Throwable e) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		Assert.assertEquals(0, ai.get());
	}

	@Test
	public void testHandleExceptionWhenShouldContinueOnException() {
		AtomicInteger ai = new AtomicInteger(0);
		try {
			TaskExecutor.getInstance().of(()->{throw new Exception("MyException");})
				.shouldContinueNextTaskIfExceptionOccurs()
				.onException(e->{}).getParentTask().then(()->{ai.incrementAndGet();})
			.start(100);
		} catch (Throwable e) {
			fail ("should not throw exception");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		Assert.assertEquals(1, ai.get());
	}
	
	@Test
	public void testHandleExceptionWhenShouldContinueOnExceptionButThrowException() {
		AtomicInteger ai = new AtomicInteger(0);
		try {
			TaskExecutor.getInstance().of(()->{throw new Exception("MyException");})
				.shouldContinueNextTaskIfExceptionOccurs()
				.shouldThrowExceptionIfOccurs()
				.onException(e->{}).getParentTask().then(()->{ai.incrementAndGet();})
			.start(100);
			fail ("should throw exception");
		} catch (Throwable e) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		Assert.assertEquals(0, ai.get());
	}

	@Test
	public void testHandleExceptionInCaseOfSubmit() {
		try {
			TaskExecutor.getInstance().of(()->{throw new Exception("MyException");}).submit();
			Thread.sleep(100);
		} catch (Throwable e) {
			fail ("should not throw exception");
		}
	}

	@Test
	public void testHandleExceptionWhenHandlerAttached() {
		try {
			TaskExecutor.getInstance().of(()->{throw new Exception("MyException");}).onException(e->{}).start(100);
		} catch (Throwable e) {
			fail ("should not throw exception");
		}
	}

	@Test
	public void testHandleExceptionDefault() {
		try {
			TaskExecutor.getInstance().of(()->{throw new Exception("MyException");}).start(100);
			fail("Should have thrown exception");
		} catch (Throwable e) {
			e.printStackTrace();
			Assert.assertTrue(e.getMessage().equals("MyException"));
		}
	}
	
}
