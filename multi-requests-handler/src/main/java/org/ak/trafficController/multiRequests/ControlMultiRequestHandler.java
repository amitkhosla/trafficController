/**
 * 
 */
package org.ak.trafficController.multiRequests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This class use case is ControlMultiRequest annotated methods handling.
 * @author amit.khosla
 */
@Named
@Aspect
public class ControlMultiRequestHandler {
	
	Logger LOGGER = Logger.getLogger(ControlMultiRequestHandler.class.getName());
	
	@Around("execution(@org.ak.trafficController.multiRequests.ControlMultiRequest * *(..)) && @annotation(controlMultiRequest))")
	public Object process(ProceedingJoinPoint joinPoint, ControlMultiRequest controlMultiRequest) throws Throwable {
		String name = getName(controlMultiRequest, joinPoint);
		if (controlMultiRequest.shouldConsiderPartial()) {
			return handlePartialFlow(joinPoint, controlMultiRequest, name);
		} else {
			return handleNormalFlow(joinPoint, controlMultiRequest, name);
		}
	}

	
	/**
	 * Handle partial flow where we want items of list are actually applied to function but rest of them should be taken from other processes.
	 * @param joinPoint Join point which will run actual method for the delta input items
	 * @param controlMultiRequest The annotation details to find what all should be considered
	 * @param name Unique name of operation
	 * @return The output, will actually be map but it is as per syntax
	 * @throws Throwable In case of issue in processing, throw throwable
	 */
	protected Object handlePartialFlow(ProceedingJoinPoint joinPoint, ControlMultiRequest controlMultiRequest, String name)
			throws Throwable {
		AtomicReference<Throwable> throwable = new AtomicReference<Throwable>(null);
		Function<List<Object>, Map<Object,Object>> function = list->{
			Object[] args = joinPoint.getArgs();
			args[0] = list;
			try {
				return (Map<Object, Object>) joinPoint.proceed(args);
			} catch (Throwable t) {
				throwable.set(t);
			}
			return null;
		};
		MultiRequestHandler handler = getHandler();
		name = getNameIfOtherParamsAvailable(joinPoint, name, handler);
		Object output = handler.processListToMap(function, (List<Object>) joinPoint.getArgs()[0], name, controlMultiRequest.reusePreviousRunResult() * 1000, !controlMultiRequest.shouldWait());
		if (throwable.get() == null) {
			return output;
		} else {
			throw throwable.get();
		}
	}


	/**
	 * Get name for the partial case where we have other params as well.
	 * @param joinPoint Join point of the call
	 * @param name Name passed
	 * @param handler Handler which will be used to find the name
	 * @return The updated name
	 */
	private String getNameIfOtherParamsAvailable(ProceedingJoinPoint joinPoint, String name,
			MultiRequestHandler handler) {
		Object[] args = joinPoint.getArgs();
		if (args.length > 1) {
			Object[] nameArgs = Arrays.copyOfRange(args, 1, args.length);
			name = handler.getName(name, nameArgs);
		}
		return name;
	}

	/**
	 * Handle the normal flow where we are expecting the input is single entity.
	 * @param joinPoint The process handle which will be used to extract results
	 * @param controlMultiRequest The annotation to control the flow
	 * @param name Unique name
	 * @return Output of the function
	 * @throws Throwable thrown if there is some issue in processing
	 */
	private Object handleNormalFlow(ProceedingJoinPoint joinPoint, ControlMultiRequest controlMultiRequest, String name) throws Throwable {
		MultiRequestHandler hanlderInstance = getHandler();
		AtomicReference<Throwable> throwable = new AtomicReference<Throwable>(null);
		name = hanlderInstance.getName(name, joinPoint.getArgs());
		Object output = hanlderInstance.process(()->{
			try {
				return joinPoint.proceed();
			} catch (Throwable t) {
				throwable.set(t);
			}
			return null;
		}, !controlMultiRequest.shouldWait(), name, controlMultiRequest.reusePreviousRunResult() * 1000, controlMultiRequest.numberOfRetries(), controlMultiRequest.retryInterval());
		if (throwable.get() != null) {
			throw throwable.get(); 
		} else {
			return output;
		}
	}

	/**
	 * Returns MultiRequestHandler instance 
	 * @return the instance of MultiRequestHandler
	 */
	protected MultiRequestHandler getHandler() {
		return MultiRequestHandler.hanlderInstance;
	}

	/**
	 * Find the unique name.
	 * @param controlMultiRequest The annotation
	 * @param joinPoint Join point
	 * @return Unique name
	 */
	protected String getName(ControlMultiRequest controlMultiRequest, ProceedingJoinPoint joinPoint) {
		String uniqueName = controlMultiRequest.uniqueName();
		if (uniqueName == null || uniqueName.isEmpty()) {
			String nameFinderMethod = controlMultiRequest.nameFinderMethod();
			if (!isEmpty(nameFinderMethod)) {
				String nameFromRunningMethod = getNameByRunningMethod(joinPoint, nameFinderMethod);
				if (!isEmpty(nameFromRunningMethod)) {
					return nameFromRunningMethod;
				}
			} 
			return joinPoint.getSignature().toString();
		}
		return uniqueName;
	}

	boolean isEmpty(String string) {
		return null == string || string.trim().isEmpty();
	}

	/**
	 * @param joinPoint
	 * @param nameFinderMethod
	 * @return
	 */
	protected String getNameByRunningMethod(ProceedingJoinPoint joinPoint, String nameFinderMethod) {
		Object target = joinPoint.getTarget();
		return getNameByRunningMethod(nameFinderMethod, target);
	}

	Map<String, Method> methodMappings = new ConcurrentHashMap<>();

	/**
	 * @param nameFinderMethod
	 * @param target
	 * @return
	 */
	protected String getNameByRunningMethod(String nameFinderMethod, Object target) {
		Class c = target.getClass();
		String nameInCache = (c.getName() + "-" + nameFinderMethod).intern();
		Method m = methodMappings.get(nameInCache);
		if (m == null) {
			while (c!=null) {
				try {
					m = c.getDeclaredMethod(nameFinderMethod);
					methodMappings.put(nameInCache, m);
				} catch (Exception e) {
					LOGGER.fine("could not process for given class, trying for super.");
				} 
				c= c.getSuperclass();
			}
		}
		if (m == null) {
			return null;
		}
		boolean accessible = m.isAccessible();
		m.setAccessible(true);
		String output = null;
		try {
			output = (String) m.invoke(target);
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Could not execute the method", e);
		}
		if (!accessible) {
			m.setAccessible(accessible);
		}
		return output;
	}
	
}
