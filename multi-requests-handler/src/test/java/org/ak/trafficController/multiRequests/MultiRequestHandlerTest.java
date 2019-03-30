/**
 * 
 */
package org.ak.trafficController.multiRequests;

import static org.junit.Assert.fail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author amit.khosla
 *
 */
public class MultiRequestHandlerTest {

	
	@Test
	public void testGetUniqueName() { //the unique name will be class name + > + line number
		MultiRequestHandler handler = new MultiRequestHandler();
		Assert.assertEquals(this.getClass().getName()+" > 33", handler.getUniqueName()); //make sure that 25 is line number. If this moves to other line, we need to update this.
	}
	
	@Test
	public void testProcessRunnable() {
		AtomicBoolean called = new AtomicBoolean(false);
		MultiRequestHandler handler = new MultiRequestHandler();
		handler.process(()->called.set(true), false);
		Assert.assertTrue(called.get());
	}
	
	@Test
	public void testprocessRunnableMultipleRuns() {
		AtomicInteger ai = new AtomicInteger(0);
		MultiRequestHandler handler = new MultiRequestHandler();
		
		Runnable r = ()-> {
			handler.process(()->{
				ai.incrementAndGet();
				try {
					Thread.sleep(1000);
					System.out.println("Me done....");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}, false);
			
		};
		List<Thread> ts = new ArrayList<>();
		int numberOfThreads = 1000;
		for (int i=0;i<numberOfThreads; i++) {
			ts.add(new Thread(r));
		}
		for (int i=0;i<numberOfThreads; i++) {
			ts.get(i).start();
		}
		for (int i=0;i<numberOfThreads; i++) {
			try {
				ts.get(i).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Assert.assertEquals(1, ai.get());
		
	}
	
	@Test
	public void testprocessSupplierMultipleRuns() {
		AtomicInteger ai = new AtomicInteger(0);
		MultiRequestHandler handler = new MultiRequestHandler();
		handler.list.setConsumer(m->{});
		Runnable r = ()-> {
			//try {
				Object processResult = handler.process("name", ()->{
					//ai.incrementAndGet();
					try {
						Thread.sleep(1000);
						System.out.println("Me done....");
						return 1;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return 0;
				} );
				ai.addAndGet((Integer)processResult);
			/*} catch (Exception e) {
				e.printStackTrace();
			}*/
		};
		List<Thread> ts = new ArrayList<>();
		int numberOfThreads = 1000;
		for (int i=0;i<numberOfThreads; i++) {
			ts.add(new Thread(r));
		}
		for (int i=0;i<numberOfThreads; i++) {
			ts.get(i).start();
			/*try {
				Thread.sleep(10l);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
		for (int i=0;i<numberOfThreads; i++) {
			try {
				ts.get(i).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Assert.assertEquals(1000, ai.get());
		
	}
	
	@Test
	public void testprocessFlow() {
		MultiRequestHandler handler = new MultiRequestHandler();
		AtomicInteger ai = new AtomicInteger(0);
		Supplier supplier = ()->{
			if (ai.incrementAndGet() < 4) {
				throw new RuntimeException();
			} else {
				return null;
			}
		};
		handler.processFlow(supplier,"name", 10L, 5, 1);
		Assert.assertEquals(4, ai.get());
		
		ai.set(0);
		try {
			handler.processFlow(supplier,"name", 10L,2, 1);
			fail("Should have thrown exception");
		} catch (RuntimeException e) {
			Assert.assertEquals(3, ai.get()); //3 times should have called.
		}
	}
	
	@Test
	public void testgetName() {
		MultiRequestHandler handler = new MultiRequestHandler();
		Assert.assertEquals("myName" + ((char)1), handler.getName("myName"));
		Assert.assertEquals("myName" + ((char)1), handler.getName("myName", null));
		Assert.assertEquals("myName"+ ((char)1) +"2"+((char)2)+"4" + ((char)2), handler.getName("myName", 2 , 4));
		MultiRequestDTO obj = new MultiRequestDTO().setName("myREQName");
		String name = handler.getName("myName", 2 , 4, obj);
		Assert.assertTrue(name.startsWith("myName" + ((char)1) + "2"+((char)2)+"4" + ((char)2)));
		Assert.assertTrue(name.contains("myREQName"));
	}
	
	@Test
	public void testwaitingFlow() {
		MultiRequestHandler hanlderInstance = MultiRequestHandler.hanlderInstance;
		Assert.assertNull(hanlderInstance.waitingFlow(()->1, true, "myName", 100l));
		Assert.assertEquals(1,hanlderInstance.returnedWithoutWaitingToExecutedAsAsync.get());
		
		StringBuilder sb = new StringBuilder();
		Assert.assertEquals(1, hanlderInstance.waitingFlow(()->1, false, "myName", 10000l).intValue());
		Assert.assertEquals(-1, hanlderInstance.returnedWithoutExecution.get());
		
		Assert.assertEquals(1, hanlderInstance.waitingFlow(()->2, false, "myName", 100l).intValue());
		Assert.assertEquals(0, hanlderInstance.returnedWithoutExecution.get());
	}
	
	@Test
	public void testProcessListFunction() {
		List<String> list = new ArrayList<>();
		for (int i=0;i<100;i++) {
			list.add((i+1) + "");
		}
		
		Map<String, AtomicInteger> aiMap = new HashMap<>();
		list.forEach(l->{
			aiMap.put(l, new AtomicInteger());
		});
		
		Function<List<String>, Map<String, Integer>> function = l-> {
			Map<String, Integer> map = new HashMap<String, Integer>();
			l.forEach(s->{
				map.put(s, Integer.valueOf(s));
				aiMap.get(s).incrementAndGet();
			});
			return map;
		};
		
		Map<String, Integer> output = MultiRequestHandler.hanlderInstance.processListToMap(function , list, "myName", 1000l);
		list.forEach(l->{
			Assert.assertEquals(Integer.valueOf(l), output.get(l));
			Assert.assertEquals(1, aiMap.get(l).get());
		});
		
		Map<String, Integer> newOutput = MultiRequestHandler.hanlderInstance.processListToMap(function , list, "myName", 1000l);
		list.forEach(l->{
			Assert.assertEquals(Integer.valueOf(l), newOutput.get(l));
			Assert.assertEquals(1, aiMap.get(l).get());
		});
		
		Map<String, Integer> newerOutput = MultiRequestHandler.hanlderInstance.processListToMap(function , list, "myName1", 1000l);
		list.forEach(l->{
			Assert.assertEquals(Integer.valueOf(l), newerOutput.get(l));
			Assert.assertEquals(2, aiMap.get(l).get());
		});
		
	}

	@Test
	public void testWaiting() {
		List<String> list = new ArrayList<>();
		for (int i=0;i<100;i++) {
			list.add((i+1) + "");
		}
		
		Map<String, AtomicInteger> aiMap = new HashMap<>();
		list.forEach(l->{
			aiMap.put(l, new AtomicInteger());
		});
		
		Function<List<String>, Map<String, Integer>> function = l-> {
			Map<String, Integer> map = new HashMap<String, Integer>();
			l.forEach(s->{
				map.put(s, Integer.valueOf(s));
				aiMap.get(s).incrementAndGet();
			});
			return map;
		};
		Thread t1= new Thread(()->{
			Function<List<String>, Map<String, Integer>> function123 = l-> {
				Map<String, Integer> map = new HashMap<String, Integer>();
				l.forEach(s->{
					map.put(s, Integer.valueOf(s));
					aiMap.get(s).incrementAndGet();
				});
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return map;
			};
			
			Map<String, Integer> otherOutput = MultiRequestHandler.hanlderInstance.processListToMap(function123 , list, "myName1234", 100000l);
		});
		
		t1.start();
		try {
			Thread.sleep(100L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String, Integer> newerOutput2 = MultiRequestHandler.hanlderInstance.processListToMap(function , list, "myName1234", 1000l, true);
		list.forEach(s->{
			Assert.assertNull(newerOutput2.get(s));
		});
		Assert.assertEquals(list.size(), newerOutput2.size());
	}
	
	@Test
	public void testSingleRecordConsumerForList() {
		List<String> list = getDummyList(1,100);
		
		Map<String, AtomicInteger> aiMap = new HashMap<>();
		list.forEach(l->{
			aiMap.put(l, new AtomicInteger());
		});
		Function<String, Integer> function = s->{
			aiMap.get(s).incrementAndGet();
			return Integer.valueOf(s);
		};
		Map<String, Integer> newerOutput2 = MultiRequestHandler.hanlderInstance
				.processListByConvertingIndividualRecords(function, list, "uniqueName", 10000L);
		list.forEach(i->{
			Assert.assertEquals(Integer.valueOf(i), newerOutput2.get(i));
		});
		list.clear();
		list.addAll(getDummyList(50, 200));
		for(int i=101;i<=200;i++) {
			aiMap.put(i + "", new AtomicInteger());
		}
		newerOutput2.clear();
		newerOutput2.putAll(MultiRequestHandler.hanlderInstance
				.processListByConvertingIndividualRecords(function, list, "uniqueName", 10000L));
		
		list.forEach(i->{
			Assert.assertEquals(Integer.valueOf(i), newerOutput2.get(i));
		});
		list.clear();
		list.addAll(getDummyList(1, 200));
		list.forEach(i->{
			Assert.assertEquals(1,aiMap.get(i).get());
		});
	}
	
	
	@Test
	public void testpopulateFromWaiting() {
		MultiRequestHandler.hanlderInstance.ran.put("k", new MultiRequestDTO().setOutput("v").setTime(1000000l).setName("k"));
		MultiRequestHandler.hanlderInstance.running.put("k1", LocalDateTime.now());
		Thread t = new Thread(()->{
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			MultiRequestHandler.hanlderInstance.running.remove("k1");
		});
		t.start();
		Map<String, String> waiting = new HashMap<>();
		waiting.put("k", "asd");
		waiting.put("k1", "asd1");
		Map<String, String> result = new HashMap<>();
		Map<String, String> returnedNull = new HashMap<>();
		MultiRequestHandler.hanlderInstance.populateFromWaiting(waiting, result, returnedNull);
	}

	/**
	 * @param endIndex
	 * @return
	 */
	private List<String> getDummyList(int startIndex, int endIndex) {
		List<String> list = new ArrayList<>();
		for (int i=startIndex;i<=endIndex;i++) {
			list.add(i + "");
		}
		return list;
	}
}
