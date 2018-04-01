/**
 * 
 */
package org.ak.trafficController.annotations.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.ApplicationContext;

/**
 * @author amit.khosla
 *
 */
@Named
public class MethodInvoker {
	/**
	 * Context to find the class from spring context.
	 */
	@Inject
	ApplicationContext context;
	
	protected Object getObjectHavingMethod(Class consumerClass) throws InstantiationException, IllegalAccessException {
		Map<String, Object> beansPresent = context.getBeansOfType(consumerClass);
		if (beansPresent.size() > 0) {
			return beansPresent.values().iterator().next();
		}
		return consumerClass.newInstance();
	}
	
	public Object extractData(Class consumerClass, String method) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
		Object objectToWorkOn = getObjectHavingMethod(consumerClass);
		Method m = consumerClass.getMethod(method);
		return m.invoke(objectToWorkOn);
	}
	
	public void executeMethod(Class consumerClass, String method, Object param) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object objectToWorkOn = getObjectHavingMethod(consumerClass);
		for (Method m : consumerClass.getMethods()) {
			if (m.getName().equals(method) && m.getParameterCount() == 1) {
				m.invoke(objectToWorkOn, param);
				return;
			}
		}
	}
}
