package org.ak.trafficController.annotations.samples.normalFlow;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.annotations.api.Submit;
import org.ak.trafficController.annotations.api.TaskType;

@Named
public class PersistValueInCache {
	
	@Inject
	LocalCacheHandler localCacheHandler;
	
	@Inject
	RedisCacheHandler redisCacheHandler;
	
	@Submit
	public void persistValueInLocalCacheAfterManipulations(String key, double value) {
		Object object = manipulateForLocalCache(key, value);
		localCacheHandler.save(object);
	}
	
	private Object manipulateForLocalCache(String key, double value) {
		System.out.println("some calculations for creating the object..");
		return new Object();
	}
	private Object manipulateForRedisCache(String key, double value) {
		System.out.println("some calculations for creating the object for redis..");
		return new Object();
	}

	@Submit
	public void persistValueInRedisCacheAfterManipulations(String key, double value) {
		Object object = manipulateForRedisCache(key, value);
		redisCacheHandler.persistInRedis(object);
	}
	
	
	
}
