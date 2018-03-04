/**
 * 
 */
package org.ak.trafficController.messaging.mem;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for tuning all configured dynamic queues.
 * By tuning we mean that it will automatically add consumers, reduce consumers or clear queue as per user requirement.
 * This class just facilitates {@code DynamicSettings} to actually tune.
 * @author Amit Khosla
 *
 */
public class InMemoryQueueTuner {
	
	/**
	 * Queue managers added to the tuner.
	 */
	List<InMemoryQueueManager> queueManagers = new ArrayList<>();

	/**
	 * Dynamic settings added to the tuner.
	 */
	List<DynamicSettings> dynamicSettings = new ArrayList<>();
	
	/**
	 * Sleep interval, post which tuning will be retried.
	 */
	Long sleepInterval = 1000l;

	protected boolean shouldContinue = true;
	
	/**
	 * Constructor of InMemoryTuner which also registers managers which will be tuned.
	 * @param managers Managers to be registered to tuner
	 */
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
	
	/**
	 * Tuning process. Tune all registered managers and settings.
	 */
	public void tune() {
		queueManagers.parallelStream().forEach(this::tune);
		dynamicSettings.parallelStream().forEach(setting->setting.adjust());
	}
	
	/**
	 * Tune for a single manager.
	 * @param manager queue manager
	 */
	public void tune(InMemoryQueueManager manager) {
		manager.dynamicSettings.values().forEach(this::tune);
	}
	
	/**
	 * Tune a Dynamic setting.
	 * @param dynamicSettings to be tuned
	 */
	public void tune(DynamicSettings dynamicSettings) {
		dynamicSettings.adjust();
	}

	/**
	 * Get configured sleep time.
	 * @return Sleep time after each tuning will perform
	 */
	public Long getSleepInterval() {
		return sleepInterval;
	}

	/**
	 * Set sleep interval post which tuning will perform.
	 * @param sleepInterval Sleep interval to be set
	 * @return This tuner object to further use.
	 */
	public InMemoryQueueTuner setSleepInterval(Long sleepInterval) {
		this.sleepInterval = sleepInterval;
		return this;
	}
	
	/**
	 * Shutdown tuner.
	 */
	public void shutdown() {
		this.shouldContinue = false;
	}
}
