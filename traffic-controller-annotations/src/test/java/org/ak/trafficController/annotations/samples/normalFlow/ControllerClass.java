package org.ak.trafficController.annotations.samples.normalFlow;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;

@Named
public class ControllerClass {
	
	@Inject
	DataCollectionService dataCollectionService;
	
	@Inject
	PersistValueInCache cacheHandler;
	
	@Inject
	DataWorkerService dataWorkerService;
	
	@Controlled
	public void doSomeOperation() {
		double value = dataCollectionService.getData(234);
		cacheHandler.persistValueInLocalCacheAfterManipulations("somekey", value); //this will start running in parallel with next line
		cacheHandler.persistValueInRedisCacheAfterManipulations("somekey", value); //this will start running in parallel with previous line
		try {
			dataWorkerService.doSomeThingWithData("someKey", value);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("I have caught exception");
		}
	}
}
