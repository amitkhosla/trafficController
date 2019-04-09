/**
 * 
 */
package org.ak.trafficController.multiRequests;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.ak.trafficController.messaging.mem.InMemoryQueue;

/**
 * @author amit.khosla
 *
 */
public class WaitsHandler {
	InMemoryQueue<Supplier<Boolean>> suppliers = new InMemoryQueue<>("WaitingsHandlerQueue");
	
	public void initialize() {
		int processors = Runtime.getRuntime().availableProcessors();
		int half = processors/2;
		if (half == 0) {
			half = 1;
		}
		Consumer<List<Supplier<Boolean>>> consumer = list ->{
			LinkedList<Supplier<Boolean>> stillRunning  = new LinkedList<>();
			list.forEach(s->{
				boolean shouldRunFurther = s.get();
				if (shouldRunFurther) {
					stillRunning.add(s);
				}
			});
			suppliers.addAllFromCollection(stillRunning);
		};
		suppliers.setBatchSize(100000).setBatchConsumer(consumer).setSleepTimePostConsumingAllMessage(2l).setBatchConsumerCount(half);
	}
}
