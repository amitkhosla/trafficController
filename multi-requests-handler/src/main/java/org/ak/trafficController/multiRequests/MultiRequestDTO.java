/**
 * 
 */
package org.ak.trafficController.multiRequests;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author amit.khosla
 *
 */
public class MultiRequestDTO {
	private long time;
	private String name;
	private LocalDateTime timeWhenStarted;
	private LocalDateTime expiryTime;
	public static Object INVALIDATED_OBJECT = new Object(); 
	
	/**
	 * The expiration time of the object.
	 * @return the expiryTime
	 */
	public LocalDateTime getExpiryTime() {
		return expiryTime;
	}
	/**
	 * This method sets expiryTime with the value passed.
	 * @param expiryTime the expiryTime to set
	 * @return Self to use further
	 */
	public MultiRequestDTO setExpiryTime(LocalDateTime expiryTime) {
		this.expiryTime = expiryTime;
		return this;
	}
	private Object output;
	private RuntimeException exception;
	
	
	/**
	 * This returns the exception if occurred.
	 * @return the exception
	 */
	public RuntimeException getException() {
		return exception;
	}
	/**
	 * This method sets exception with the value passed.
	 * @param exception the exception to set
	 * @return Self to use further
	 */
	public MultiRequestDTO setException(RuntimeException exception) {
		this.exception = exception;
		return this;
	}
	/**
	 * Returns the output, the actual value. If expired, returnd invalidated object.
	 * @return the output
	 */
	public Object getOutput() {
		if (LocalDateTime.now().isAfter(expiryTime)) {
			return INVALIDATED_OBJECT;
		}
		return output;
	}
	/**
	 * This method sets output with the value passed.
	 * @param output the output to set
	 * @return Self to use further
	 */
	public MultiRequestDTO setOutput(Object output) {
		this.output = output;
		return this;
	}
	/**
	 * Return the expiration time.
	 * @return the time
	 */
	public long getTime() {
		return time;
	}
	/**
	 * This method sets time with the value passed.
	 * @param time the time to set
	 * @return Self to use further
	 */
	public MultiRequestDTO setTime(long time) {
		this.time = time;
		LocalDateTime newTime = this.getTimeWhenStarted().plus(time, ChronoUnit.MILLIS);
		this.expiryTime = getApplicableLocalDateTime(newTime);
		return this;
	}
	/**
	 * Get applicableTimeWhenWeExpire by removing nanos etc.
	 * @param newTime The time to be transformed
	 * @return The applicable time
	 */
	protected static LocalDateTime getApplicableLocalDateTime(LocalDateTime newTime) {
		return LocalDateTime.of(newTime.getYear(), newTime.getMonthValue(), newTime.getDayOfMonth(), newTime.getHour(), newTime.getMinute(), newTime.getSecond());
	}
	/**
	 * Name of the data against which we are utilizing it.
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * This method sets name with the value passed.
	 * @param name the name to set
	 * @return Self to use further
	 */
	public MultiRequestDTO setName(String name) {
		this.name = name;
		return this;
	}
	/**
	 * Get the time on basis of which we want expiration time to be calculated.
	 * @return the time When we populated
	 */
	public LocalDateTime getTimeWhenStarted() {
		if (timeWhenStarted == null) {
			timeWhenStarted = LocalDateTime.now();
		}
		return timeWhenStarted;
	}
	/**
	 * This method sets timeWhenCompleted with the value passed.
	 * @param timeWhenCompleted the timeWhenCompleted to set
	 * @return Self to use further
	 */
	public MultiRequestDTO setTimeWhenStarted(LocalDateTime timeWhenCompleted) {
		this.timeWhenStarted = timeWhenCompleted;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MultiRequestDTO)) {
			return false;
		}
		
		MultiRequestDTO mr = (MultiRequestDTO) obj;
		return this.getName().equals(mr.name);
	}
}
