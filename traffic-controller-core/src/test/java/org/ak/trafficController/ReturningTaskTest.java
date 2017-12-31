package org.ak.trafficController;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.ak.trafficController.Task.TaskType;
import org.ak.trafficController.pool.ObjectPoolManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ReturningTaskTest {

	@Test
	public void testExecutableTask() {
		AtomicInteger ai = new AtomicInteger();
		Supplier<Integer> runnable = ()->ai.incrementAndGet();
		ReturningTask<Integer> rt = ReturningTask.getFromPool(234, runnable, TaskType.NORMAL);
		Assert.assertTrue(rt.taskType == TaskType.NORMAL);
		Assert.assertEquals(234,rt.uniqueNumber.intValue());
		Assert.assertFalse(rt.canSendBackToPool());
		
		rt.executeCurrentTask();
		Assert.assertEquals(1, rt.get().intValue());
	}
	
	@Test
	public void testThenConsume() {
		ExecutableTask mock = Mockito.mock(ExecutableTask.class);
		StringBuilder sb = new StringBuilder();
		ReturningTask<Integer> rt = new ReturningTask<Integer>(1234, ()->2, TaskType.NORMAL) {
			@Override
			protected ExecutableTask getConsumeExecutableTask(
					Consumer<Integer> consumer, TaskType tp) {
				sb.append("called with task type " + tp);
				return mock;
			}
		};
		ExecutableTask et = rt.thenConsume(i->{});
		assertEquals(mock,et);
		assertEquals("called with task type " + TaskType.NORMAL, sb.toString());
		sb.delete(0, sb.length());
		
		et = rt.thenConsumeSlow(i->{});
		assertEquals(mock,et);
		assertEquals("called with task type " + TaskType.SLOW, sb.toString());
	}
	
	@Test
	public void testThen() {
		ReturningTask mock = Mockito.mock(ReturningTask.class);
		StringBuilder sb = new StringBuilder();
		ReturningTask<Integer> rt = new ReturningTask<Integer>(1234, ()->2, TaskType.NORMAL) {
			@Override
			protected <R> ReturningTask then(Function<Integer, R> consumer,
					TaskType tp) {
				sb.append("called with task type " + tp);
				return mock;
			}
		};
		ReturningTask et = rt.then(i->2);
		assertEquals(mock,et);
		assertEquals("called with task type " + TaskType.NORMAL, sb.toString());
		sb.delete(0, sb.length());
		
		et = rt.thenSlow(i->3);
		assertEquals(mock,et);
		assertEquals("called with task type " + TaskType.SLOW, sb.toString());
	}
	
	@Test
	public void testThenCore() {
		TaskExecutor.instance = new TaskExecutor() {
			@Override
			public void enque(Task nextTask) {
				// TODO Auto-generated method stub
				nextTask.execute();
			}
		};
		AtomicInteger ai = new AtomicInteger();
		ReturningTask<Integer> rt = new ReturningTask<Integer>(1234, ()->5, TaskType.NORMAL);
		rt.taskExecutor =  TaskExecutor.instance;
		rt.then(i->i*2).thenConsume(i->{ai.set(i);});
		
		rt.execute();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(10, ai.get());
	}
	
}
