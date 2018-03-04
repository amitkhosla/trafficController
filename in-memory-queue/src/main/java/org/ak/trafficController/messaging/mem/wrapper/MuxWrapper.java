/**
 * 
 */
package org.ak.trafficController.messaging.mem.wrapper;

/**
 * This class wraps the actual object.
 * This will keep data and name of the key by which we can find. 
 * @author Amit Khosla
 */
public class MuxWrapper {
	/**
	 * Actual data object.
	 */
	private Object obj;
	/**
	 * Type of message.
	 */
	private String type;
	
	/**
	 * Get data to be consumed by actual consumer.
	 * @return Data to be consumed by actual consumer
	 */
	public Object getObj() {
		return obj;
	}
	/**
	 * Set the data in the message.
	 * @param obj Data which needs to be sent
	 * @return This object for further use
	 */
	public MuxWrapper setObj(Object obj) {
		this.obj = obj;
		return this;
	}
	/**
	 * Get key against which this message will be processed.
	 * @return Key of the message
	 */
	public String getKey() {
		return type;
	}
	/**
	 * Set the key against which this data will be processed.
	 * @param type Type of message
	 * @return This object for further use
	 */
	public MuxWrapper setKey(String type) {
		this.type = type;
		return this;
	}
}
