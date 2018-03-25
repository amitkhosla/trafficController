package com.samples.sample1.annotatedFlow;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.samples.RandomGenerator;

public class Sample1AnnotatedMain {

	public static void main(String[] args) throws IOException, InterruptedException {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigClass.class);
		PeopleBusiness business = context.getBean(PeopleBusiness.class);
		Thread.sleep(5000L);
		System.out.println("STARTING....");
		long maxSize = 150L;
		business.createPeople(maxSize);
		business.buildGraph(maxSize);
		System.out.println("About to start bad thing.");
		AtomicInteger started = new AtomicInteger(0);
		AtomicInteger completed = new AtomicInteger(0);
		AtomicLong timeTaken = new AtomicLong(0);
		ExecutorService service = Executors.newCachedThreadPool();
		//System.out.println(business.retrieveGraph(l));
		while (true) {
			int maxThreads = RandomGenerator.getRandomInteger(20);
			Long delay = RandomGenerator.getRandomLong(15_000L);
			System.out.println("starting " + maxThreads);
			List<Future> futures = new ArrayList<>();
			for (int i=0;i<maxThreads; i++) {
				futures.add(service.submit(()->{
					Long id = RandomGenerator.getRandomLong(maxSize);
					try {
						long t1 = System.currentTimeMillis();
						business.retrieveGraph(id);
						long t2 = System.currentTimeMillis();
						long time = t2-t1;
						//System.out.println("retrieved FOR " + id + " TIME TAKEN: " + time);
						timeTaken.addAndGet(time);
						completed.incrementAndGet();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}));
				started.incrementAndGet();
			}
			System.out.println("Sleeping for " + delay);
			try {
			Thread.sleep(delay);
			} catch (Exception e) {
				e.printStackTrace();
			}
			int totalThreads = ManagementFactory.getThreadMXBean().getThreadCount();
			if (completed.get() > 0)
			System.out.println("Started : " + started.get() + " Completed: " +completed.get() + " avg time : " + (timeTaken.get()/ completed.get()) + " threads: " + totalThreads);
		}
		
		//System.out.println("built....");
	}
	
	
	
	
}
