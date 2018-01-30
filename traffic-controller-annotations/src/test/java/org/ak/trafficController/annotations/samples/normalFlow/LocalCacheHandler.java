package org.ak.trafficController.annotations.samples.normalFlow;

import javax.inject.Named;

import org.ak.trafficController.annotations.api.Controlled;

@Named
public class LocalCacheHandler {

	@Controlled 
	public void save(Object object) {
		//guavaCache.put(object);
		System.out.println("saving in local cache");
	}
}
