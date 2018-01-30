package org.ak.trafficController.annotations.samples.normalFlow;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.TaskType;

@Named
public class RedisCacheHandler {
	
	@Controlled(taskType=TaskType.SLOW)
	public void persistInRedis(Object object) {
		System.out.println("putting in redis.");
		//jedis put
	}
}
