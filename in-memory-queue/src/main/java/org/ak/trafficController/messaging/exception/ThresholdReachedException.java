package org.ak.trafficController.messaging.exception;

/**
 * Threshold reached exception. Thrown by InMemoryQueue when threshold reached.
 * @author amit.khosla
 */
public class ThresholdReachedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructor of Threshold reached exception. 
	 */
	public ThresholdReachedException() {
		super("Thredhold limit reached. Cannot add more.");
	}
	
	/**
	 * Constructor of threshold reached exception.
	 * @param message Exception message
	 */
	public ThresholdReachedException(String message) {
		super(message);
	}

}
