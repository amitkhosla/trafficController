/**
 * 
 */
package org.ak.trafficController.multiRequests;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;

/**
 * @author amit.khosla
 *
 */
public class TimeSeriesListTest {

	@Test
	public void testAddItmeToItems() {
		TimeSeriesList list = new TimeSeriesList("");
		LocalDateTime beginTime = LocalDateTime.now();
		String name = "MyName1";
		MultiRequestDTO item = new MultiRequestDTO().setName(name).setExpiryTime(beginTime.plusSeconds(5));
		list.addItemToItems(item);
		
		ConcurrentLinkedQueue<MultiRequestDTO> items = list.items.get(beginTime.plusSeconds(5));
		Assert.assertEquals(1, items.size());
		
		AtomicInteger indexer = new AtomicInteger(0);
		Runnable r = ()-> {
			for (int i=0; i<100;i++) {
				list.addItemToItems(new MultiRequestDTO().setName("Name" + indexer.incrementAndGet()).setExpiryTime(beginTime.plusSeconds(5)));
			}
		};
		List<Thread> threads = new ArrayList<>();
		for (int i=0;i<100;i++) {
			threads.add(new Thread(r));
		}
		threads.forEach(t->t.start());
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		items = list.items.get(beginTime.plusSeconds(5));
		int count = 100*100;
		Assert.assertEquals(count+1, items.size());
		for (int i=0;i<count;i++) {
			Assert.assertTrue(items.contains(new MultiRequestDTO().setName("Name" + (i+1))));
		}
	}
}
