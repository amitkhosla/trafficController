package org.ak.trafficController.messaging.exception;

public class ThresholdReachedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ThresholdReachedException() {
		super("Thredhold limit reached. Cannot add more.");
	}
	
	public ThresholdReachedException(String message) {
		super(message);
	}

}
