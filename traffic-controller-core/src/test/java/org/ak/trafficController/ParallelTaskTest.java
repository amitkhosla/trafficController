package org.ak.trafficController;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.ak.trafficController.Task.TaskType;
import org.junit.Test;

public class ParallelTaskTest {


	@Test
	public void testPostTask() {
		StringBuilder sb = new StringBuilder();
		ParallelTask pt = new ParallelTask(234, TaskType.NORMAL) {
			@Override
			protected void executeNextTask() {
				sb.append("execute next task called.");
			}
		};
		pt.postTaskRun();
		assertEquals(-1,pt.tasksLeftRef.get());
		assertEquals(0,sb.length());
		
		pt.tasksLeftRef.set(1);
		pt.postTaskRun();
		assertEquals(0,pt.tasksLeftRef.get());
		assertEquals("execute next task called.",sb.toString());
	}
	
	@Test
	public void testexecuteCurrentTask() {
		ParallelTask pt = new ParallelTask(456, TaskType.NORMAL) {
			@Override
			public void executeInternalTask(Task task) {
				try {
					task.executeCurrentTask();
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			@Override
			protected void postTaskRun() {
				//for this test, i do not want the tasks to be handled in async way...
				//so this cause concurrent modification as list I was iterating in above method
				// is getting clear.
			}
		};
		StringBuilder sb = new StringBuilder();
		pt.addRunnables(()->sb.append("1 called"), ()->sb.append("2 called"));
		pt.executeCurrentTask();
		
		assertTrue(sb.indexOf("1 called") >-1);
		assertTrue(sb.indexOf("2 called") >-1);
	}
	
	@Test
	public void testAddTasks() {
		ParallelTask pt = getTaskWithTwoNormalAndSlowTasks();
		assertEquals(4, pt.tasks.size());
		for (int i=0;i<2;i++) {
			Task task = (Task) pt.tasks.get(i);
			assertEquals(TaskType.NORMAL, task.taskType);
		}
		for (int i=2;i<4;i++) {
			Task task = (Task) pt.tasks.get(i);
			assertEquals(TaskType.SLOW, task.taskType);
		}
	}


	ParallelTask getTaskWithTwoNormalAndSlowTasks() {
		ParallelTask pt = new ParallelTask(456, TaskType.NORMAL) {};
		pt.addRunnables(()->{}, ()->{});
		pt.addSlowRunnables(()->{}, ()->{});
		return pt;
	}
	
	@Test
	public void testClean() {
		ParallelTask pt = getTaskWithTwoNormalAndSlowTasks();
		assertEquals(4, pt.tasks.size());
		pt.tasksLeftRef.set(500);
		pt.clean();
		assertEquals(0, pt.tasks.size());
		assertEquals(0, pt.tasksLeftRef.get());
	}
	
}
