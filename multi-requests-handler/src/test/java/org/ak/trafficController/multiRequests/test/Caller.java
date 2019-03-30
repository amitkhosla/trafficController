/**
 * 
 */
package org.ak.trafficController.multiRequests.test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import junit.framework.Assert;

/**
 * @author amit.khosla
 *
 */
@Configuration
@ComponentScan(basePackages="org.ak.trafficController")
@EnableAspectJAutoProxy
public class Caller {
	MyClass myclass;
	
	static ApplicationContext context;
	
	@BeforeClass
	public static void initContext() {
		System.out.println("running initContext");
		context = new AnnotationConfigApplicationContext(Caller.class);
	}
	
	@Before
	public void init() {
		ApplicationContext context = new AnnotationConfigApplicationContext(Caller.class);
		myclass = context.getBean(MyClass.class);
	}
	
	@Test
	public void testSingleParamMethod() {
		AtomicInteger ai = new AtomicInteger(0);
		Runnable runnable = ()->{
			int doubleMe = myclass.doubleMe(10);
			System.out.println(doubleMe);
			ai.set(doubleMe);
		};
		
		Runnable runnable1 = ()->{
			int doubleMe = myclass.doubleMe(20);
			System.out.println(doubleMe);
			ai.set(doubleMe);
		};
		Assert.assertTrue(findTimeAndExecute(runnable) >= 1000000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable) <= 3000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable) <= 3000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable1) >= 1000000L);
		Assert.assertEquals(40, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable) <= 3000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable1) <= 3000L);
		Assert.assertEquals(40, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable) <= 3000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable1) <= 3000L);
		Assert.assertEquals(40, ai.get());
		ai.set(0);
		
	}
	
	@Test
	public void testNoParamMethod() {
		AtomicInteger ai = new AtomicInteger(0);
		Runnable runnable = ()->{
			int doubleMe = myclass.doSomeThing();
			System.out.println(doubleMe);
			ai.set(doubleMe);
		};
		
		Runnable runnable1 = ()->{
			int doubleMe = myclass.doSomeThingElse();
			System.out.println(doubleMe);
			ai.set(doubleMe);
		};
		Assert.assertTrue(findTimeAndExecute(runnable) >= 1000000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable) <= 3000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable) <= 3000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable1) >= 1000000L);
		Assert.assertEquals(40, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable) <= 3000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable1) <= 3000L);
		Assert.assertEquals(40, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable) <= 3000L);
		Assert.assertEquals(20, ai.get());
		ai.set(0);
		Assert.assertTrue(findTimeAndExecute(runnable1) <= 3000L);
		Assert.assertEquals(40, ai.get());
		ai.set(0);
	}
	
	
	@Test
	public void testreturnNextNumber() {
		AtomicInteger errors = new AtomicInteger(0);
		Runnable r= ()->{
			if(1!=myclass.returnNextNumber()) {
				errors.incrementAndGet();
			}
		};
		List<Thread> threads = new ArrayList<>();
		for (int i=0;i<100;i++) {
			threads.add(new Thread(r));
		}
		threads.forEach(t->{
			t.start();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		threads.forEach(t->{
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		Assert.assertEquals(0, errors.get());
	}
	
	@Test
	public void testPartial() {
		List<Integer> list = new ArrayList<>();
		for (int i=0;i<100;i++) {
			list.add(i);
		}
		Map<Integer, Double> map = myclass.getHalfForAll(list);
		Assert.assertEquals(100, map.size());
		for (int i=0;i<100;i++) {
			Assert.assertEquals(((double)i)/2, map.get(i));
		}
		
		list.clear();
		for (int i =50;i<200;i++) {
			list.add(i);
		}
		 map = myclass.getHalfForAll(list);
			Assert.assertEquals(150, map.size());
			for (int i=50;i<200;i++) {
				Assert.assertEquals(((double)i)/2, map.get(i));
			}
		List<Integer> list1 = myclass.clq.remove();
		for (int i=0;i<100;i++) {
			Assert.assertTrue(list1.contains(i));
		}
		
		list1 = myclass.clq.remove();
		for (int i=100;i<200;i++) {
			Assert.assertTrue(list1.contains(i));
		}
	}
	
	@Test
	public void testPartialMultiThread() {
		Random random = new Random(1);
		AtomicInteger errors = new AtomicInteger(0);
		List<Thread> threads = new ArrayList<>();
		threads.add(new Thread(getRunnable(0, 100, errors)));
		for (int i=0;i<98;i++) {
			int start = random.nextInt(500);
			if (start<0) start =0;
			int end = random.nextInt(500);
			if (end <0) end = 500;
			if (start > end) {
				threads.add(new Thread(getRunnable(end, start, errors)));
			} else {
				threads.add(new Thread(getRunnable(start, end, errors)));
			}
		}
		threads.add(new Thread(getRunnable(0, 500, errors)));
		threads.forEach(t->{
			t.start();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		threads.forEach(t->{
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		Assert.assertEquals(0, errors.get());
		for (int i=0;i<500;i++) {
			System.out.println(i + " >>>> " +  myclass.cMap.get(i).get());
			//Assert.assertEquals("for " + i + " got more." ,1, myclass.cMap.get(i).get());
		}
	}
	
	@Test
	public void testPartialMultiThreadSeq() {
		Random random = new Random(1);
		AtomicInteger errors = new AtomicInteger(0);
		List<Thread> threads = new ArrayList<>();
		for (int i=0;i<100;i++) {
			int start = i*5;
			int end = (i+1) * 5 + 5;
			threads.add(new Thread(getRunnable(start, end, errors)));
		}
		threads.forEach(t->{
			t.start();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		threads.forEach(t->{
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		Assert.assertEquals(0, errors.get());
		for (int i=0;i<500;i++) {
			System.out.println(i + " >>>> " +  myclass.cMap.get(i).get());
			Assert.assertEquals("for " + i + " got more." ,1, myclass.cMap.get(i).get());
		}
	}
	
	public Runnable getRunnable(int start, int end, AtomicInteger errors) {
		return ()->{
			List<Integer> list = new ArrayList<>();
			for (int i=start; i<=end;i++) {
				list.add(i);
			}
			Map<Integer, Integer> map = myclass.getDouble(list);
			for (int i=start;i<=end; i++) {
				if (map.get(i) != i*2) {
					errors.incrementAndGet();
				}
			}
		};
	}
	
	public static long findTimeAndExecute(Runnable runnable) {
		LocalDateTime now = LocalDateTime.now();
		runnable.run();
		LocalDateTime now1 = LocalDateTime.now();
		long timeBetween = ChronoUnit.MICROS.between(now, now1);
		System.out.println(timeBetween);
		return timeBetween;
		
	}
}
