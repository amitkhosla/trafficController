/**
 * 
 */
package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for tuning all configured tunables.
 * By tuning we mean that it will automatically add consumers, reduce consumers or clear queue as per user requirement.
 * This class just facilitates {@code DynamicSettings} to actually tune.
 * @author Amit Khosla
 *
 */
public class InMemoryQueueTuner {
	
	List<InMemoryQueueManager> queueManagers = new ArrayList<>();

	List<DynamicSettings> dynamicSettings = new ArrayList<>();
	
	Long sleepInterval = 1000l;

	protected boolean shouldContinue = true;
	
	
	public InMemoryQueueTuner(InMemoryQueueManager... managers) {
		for (InMemoryQueueManager manager : managers) {
			queueManagers.add(manager);
		}
	}
	

	/**
	 * This method starts a tuning thread.
	 * The tuning thread tune all attached managers' queues.
	 */
	public void startTuning() {
		Thread tuningThread = new Thread(this::tuneProcess, "tuningThread");
		tuningThread.start();
	}
	
	/**
	 * This is the actual method being called from the thread.
	 */
	protected void tuneProcess() {
		while (shouldContinue) {
			tune();
			DynamicSettings.sleep(sleepInterval);
		}
	}
	
	public void tune() {
		queueManagers.parallelStream().forEach(this::tune);
		dynamicSettings.parallelStream().forEach(setting->setting.adjust());
	}
	
	public void tune(InMemoryQueueManager manager) {
		manager.dynamicSettings.values().forEach(this::tune);
	}
	
	public void tune(DynamicSettings dynamicSettings) {
		dynamicSettings.adjust();
	}

	public Long getSleepInterval() {
		return sleepInterval;
	}

	public InMemoryQueueTuner setSleepInterval(Long sleepInterval) {
		this.sleepInterval = sleepInterval;
		return this;
	}
	
	public void shutdown() {
		this.shouldContinue = false;
	}
}
