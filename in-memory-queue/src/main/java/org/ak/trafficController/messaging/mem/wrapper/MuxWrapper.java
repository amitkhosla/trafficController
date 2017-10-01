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
	private Object obj;
	private String type;
	
	public Object getObj() {
		return obj;
	}
	public MuxWrapper setObj(Object obj) {
		this.obj = obj;
		return this;
	}
	public String getKey() {
		return type;
	}
	public MuxWrapper setKey(String type) {
		this.type = type;
		return this;
	}
}
