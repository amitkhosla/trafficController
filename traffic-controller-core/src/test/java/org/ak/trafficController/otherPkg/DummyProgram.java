package org.ak.trafficController.otherPkg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.ak.trafficController.TaskExecutor;

public class DummyProgram {
	public static void main(String[] args) {
		ConcurrentLinkedQueue<Integer> clq = new ConcurrentLinkedQueue<>();
		TaskExecutor taskExecutor = TaskExecutor.getInstance();
		List<Thread> threads = new ArrayList<>();
		int max = 30000;
		for (int i=0;i<max; i++) {
			int k = i;
			Thread thread = new Thread(()->{
				process(taskExecutor, k, clq);
			//	System.out.println("done with " + Thread.currentThread());
			});
			threads.add(thread);
			thread.start();
		}
		
		for (Thread t:threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		AtomicInteger aa = new AtomicInteger(0);
		System.out.println(clq.size());
		for (int i=0;i<max;i++) {
			boolean contains = clq.contains(23 + 32 + i);
			if (!contains) {
				System.out.println("not found... for " + i);
				aa.incrementAndGet();
			}
		}
		
		System.out.println("Issue with " + aa.get());
	}

	protected static void process(TaskExecutor taskExecutor, int i, ConcurrentLinkedQueue<Integer> clq) {
		taskExecutor.of(()->{System.out.println("starting " + i);}).thenParallel(()->23, ()->32).join(l->{
			int total = 0;
			for(int item : l) {
				total += item;
			}
			return total;
		}).thenConsume(k->{
			System.out.println("I found. for " + i + ".." + (k + i));
			clq.add((k+i));
		}).start();
	}
}
