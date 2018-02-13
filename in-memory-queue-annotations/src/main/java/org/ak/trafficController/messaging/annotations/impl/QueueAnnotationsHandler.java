package org.ak.trafficController.messaging.annotations.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.messaging.annotations.Consumer;
import org.ak.trafficController.messaging.annotations.Queued;
import org.ak.trafficController.messaging.mem.DynamicSettings;
import org.ak.trafficController.messaging.mem.InMemoryQueueManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

@Aspect
@Named
public class QueueAnnotationsHandler {
	
	@Inject
	ApplicationContext context;
	
	private static final String RETURN = "RETURN";

	private static final String CONSUMER = "CONSUMER";

	Logger logger = Logger.getLogger(QueueAnnotationsHandler.class.getName());
	
	InMemoryQueueManager manager = new InMemoryQueueManager();

	Map<Method, String> queueNameMapping = new ConcurrentHashMap<>();
	Map<Method, Boolean> queueMethodsProcessedForConsumer = new ConcurrentHashMap<>();
	Map<Method, Boolean> consumeMethodsProcessed = new ConcurrentHashMap<>();
	

	@Around("execution(@org.ak.trafficController.messaging.annotations.Queued * *(..)) && @annotation(queued)")
	public Object addToQueue(ProceedingJoinPoint joinPoint, Queued queued) throws Throwable {
		//System.out.println("called....");
		Object obj = null;
		try {
			obj = joinPoint.proceed();
		} catch (Throwable throwable) {
			logger.log(Level.WARNING,"Exception occured in running joinpoint", throwable);
			throw throwable;
		}
		if (!Objects.isNull(obj)) {
			String queueName = getQueueName(queued.name(), joinPoint, queued.itemInCollection(), RETURN);
			if (queued.itemInCollection()) {
				manager.addAndRegisterIfRequiredForCollection(queueName, (Collection) obj);
			} else {
				manager.addAndRegisterIfRequired(queueName, obj);
			}
			processConsumerDetails(queued, joinPoint, obj, queueName);
		}
		return obj;
	}

	protected void processConsumerDetails(Queued queued, ProceedingJoinPoint joinPoint, Object obj, String queueName) throws InstantiationException, IllegalAccessException {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		if (queueMethodsProcessedForConsumer.containsKey(method)) {
			return;
		}
		if (queued.consumerClass() == Queued.class) {
			queueMethodsProcessedForConsumer.put(method, false);
			return;
		}
		
		Method actualMethod = getActualConsumerMethod(queued, obj, signature, method);
		
		registerConsumerForQueued(queued, queueName, method, actualMethod);
	}

	/**
	 * @param queued
	 * @param queueName
	 * @param method
	 * @param actualMethod
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	protected void registerConsumerForQueued(Queued queued, String queueName, Method method, Method actualMethod)
			throws InstantiationException, IllegalAccessException {
		if (actualMethod == null) {
			queueMethodsProcessedForConsumer.put(method, false);
			logger.warning("Could not find method "+ queued.consumerMethod() + " in the class " + queued.consumerClass() );
			return;
		}
		if (actualMethod.isAnnotationPresent(Consumer.class)) {
			Consumer queueConsumer = actualMethod.getAnnotation(Consumer.class);
			initializeQueue(queueConsumer, getObjectHavingMethod(queued.consumerClass()), actualMethod, queued.listConsumer(), queueName);
		} else {
			initializeQueueConsumer(getObjectHavingMethod(queued.consumerClass()), actualMethod, queued.listConsumer(), queueName, 1, 10);
		}
		queueMethodsProcessedForConsumer.put(method, true);
	}

	/**
	 * @param queued
	 * @param obj
	 * @param signature
	 * @param method
	 * @return
	 */
	protected Method getActualConsumerMethod(Queued queued, Object obj, MethodSignature signature, Method method) {
		List<Method> matchingMethods = getProbableMethods(queued, method);
	
		boolean directConsumer = !queued.listConsumer();
		Method actualMethod = null;
		if (matchingMethods.size() == 1) {
			actualMethod = matchingMethods.get(0);
		} else if(obj == null || (queued.itemInCollection() && ((Collection)obj).isEmpty())) { 
			Class cls = getTypeForAnnotatedMethodReturnOrFirstParameter(queued.itemInCollection(), RETURN, signature, method);
			actualMethod = getAppropriateMethod(cls, matchingMethods, directConsumer);
		}else if (!queued.itemInCollection()) {
			actualMethod = getAppropriateMethod(obj.getClass(), matchingMethods, directConsumer);
		} else {
			Collection col = (Collection)obj;
			actualMethod = getAppropriateMethod(((Collection)obj).iterator().next().getClass(), matchingMethods, directConsumer);
		}
		return actualMethod;
	}

	protected Method getAppropriateMethod(Class obj, List<Method> matchingMethods,
			boolean directConsumer) {
		 Method actualMethod = null;
		if (directConsumer) {
			for (Method m : matchingMethods) {
				if (isOfTypeRequired(obj, m)) {
					actualMethod = m;
					break;
				}
			}
		} else {
			for (Method m : matchingMethods) {
				if (isOfTypeRequiredCollection(obj, m)) {
					actualMethod = m;
					break;
				}
			}
		}
		return actualMethod;
	}

	protected boolean isOfTypeRequiredCollection(Class cls, Method m) {
		return getGenericTypeFromParam(m).isAssignableFrom(cls);
	}

	protected Object getObjectHavingMethod(Class consumerClass) throws InstantiationException, IllegalAccessException {
		Map<String, Object> beansPresent = context.getBeansOfType(consumerClass);
		if (beansPresent.size() > 0) {
			return beansPresent.values().iterator().next();
		}
		return consumerClass.newInstance();
	}

	protected boolean isOfTypeRequired(Class cls, Method m) {
		return m.getParameterTypes()[0].isAssignableFrom(cls);
	}

	protected List<Method> getProbableMethods(Queued queued, Method method) {
		List<Method> matchingMethods = new ArrayList<>();
		Method[] methods = queued.consumerClass().getDeclaredMethods();
		for (Method m : methods) {
			if (m.getName().equals(queued.consumerMethod()) && m.getParameterTypes().length == 1) {
				matchingMethods.add(m);
			}
		}
		return matchingMethods;
	}

	protected String getQueueName(String name, ProceedingJoinPoint joinPoint, boolean shouldVerifyBatch, String producerOrConsumer) {
		if (StringUtils.isEmpty(name)) {
			Class returnType =null;
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Method method = signature.getMethod();
			String nameFromCache = queueNameMapping.get(method);
			if (!StringUtils.isEmpty(nameFromCache)) {
				return nameFromCache;
			}
			returnType = getTypeForAnnotatedMethodReturnOrFirstParameter(shouldVerifyBatch, producerOrConsumer,
					signature, method);
			String output = checkPrimitive(returnType);
			queueNameMapping.put(method, output);
			return output;
		}
		return name;
	}

	/**
	 * @param shouldVerifyBatch
	 * @param producerOrConsumer
	 * @param signature
	 * @param method
	 * @return
	 */
	protected Class getTypeForAnnotatedMethodReturnOrFirstParameter(boolean shouldVerifyBatch,
			String producerOrConsumer, MethodSignature signature, Method method) {
		Class returnType;
		if (!shouldVerifyBatch) {
			returnType = getTypeForDirectFlow(producerOrConsumer, signature);
		} else {
			returnType = getTypeForCollectionFlow(producerOrConsumer, method);
		}
		return returnType;
	}

	protected String checkPrimitive(Class returnType) {
		String output;
		if (returnType.isPrimitive()) {
			output = getWrapperForPrimitive(returnType.getName());
		} else {
			output = returnType.getName();
		}
		return output;
	}

	protected Class getTypeForCollectionFlow(String producerOrConsumer, Method method) {
		Class returnType;
		//batch so we need to verify the method signatures.
		if (RETURN.equalsIgnoreCase(producerOrConsumer)) {
			ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
			returnType = (Class) type.getActualTypeArguments()[0];
		} else {
			returnType = getGenericTypeFromParam(method);
		}
		return returnType;
	}

	protected Class getGenericTypeFromParam(Method method) {
		Class returnType;
		ParameterizedType type = (ParameterizedType) method.getParameters()[0].getParameterizedType();
		returnType =  (Class)type.getActualTypeArguments()[0];
		return returnType;
	}

	protected Class getTypeForDirectFlow(String producerOrConsumer, MethodSignature signature) {
		Class returnType;
		if (RETURN.equalsIgnoreCase(producerOrConsumer)) {
			returnType = signature.getReturnType();
		} else {
			returnType = signature.getParameterTypes()[0];
		}
		return returnType;
	}
	
	static Map<String, Class> wrappersMapping = new HashMap<>();
	static {
		wrappersMapping.put("int", Integer.class);
		wrappersMapping.put("float", Float.class);
		wrappersMapping.put("double", Double.class);
		wrappersMapping.put("long", Long.class);
		wrappersMapping.put("boolean", Boolean.class);
	}
	
	protected String getWrapperForPrimitive(String name) {
		Class class1 = wrappersMapping.get(name);
		return class1 != null ? class1.getName() : null;
	}

	@Around("execution(@org.ak.trafficController.messaging.annotations.Consumer * *(..)) && @annotation(queueConsumer)")
	public Object consume(ProceedingJoinPoint joinPoint, Consumer queueConsumer) throws Throwable {
		//System.out.println("called2....");
		Object objectHavingMethod = joinPoint.getTarget();
		MethodSignature signature = ((MethodSignature) joinPoint.getSignature());
		Method method = signature.getMethod();
		if (!consumeMethodsProcessed.containsKey(method)) {
			boolean batch = queueConsumer.batch();
			String queueName = getQueueName(queueConsumer.name(), joinPoint, batch, CONSUMER);
			initializeQueue(queueConsumer, objectHavingMethod, method, batch, queueName);
			consumeMethodsProcessed.put(method, true);
		}
		return joinPoint.proceed();
	}

	protected void initializeQueue(Consumer queueConsumer, Object objectHavingMethod, Method method, boolean batch,
			String queueName) {
		if ((batch && manager.getBatchConsumerCount(queueName) > 0) || ( !batch && manager.getDirectConsumerCount(queueName) > 0)) {
			logger.info("Already registerd for this type of queue. So skipping.");
				return;
		}
		
		int numberOfConsumers = queueConsumer.numberOfConsumers();
		int batchSize = queueConsumer.batchSize();
		
		initializeQueueConsumer(objectHavingMethod, method, batch, queueName, numberOfConsumers, batchSize);
		setDynamicNatureIfPresent(queueConsumer, batch, queueName);
	}

	/**
	 * @param queueConsumer
	 * @param batch
	 * @param queueName
	 */
	protected void setDynamicNatureIfPresent(Consumer queueConsumer, boolean batch, String queueName) {
		if (queueConsumer.dynamicNature()) {
			DynamicSettings dt = manager.setDynamic(queueName)
				.setHighLimitWhenToIncreaseConsumer(queueConsumer.numberOfMessagesInQueueWhenNewConsumerShouldBeCreated())
				.setLowLimitWhenToDecreseConsumer(queueConsumer.numberOfMessagesInQueueWhenShouldTryToReduceConsumers())
				.setShouldStopAddingAtThreshold(queueConsumer.shouldStopAddingAtThreshold())
				.setShouldClearQueueAtThreshold(queueConsumer.shoulClearOnThreshold())
				.setShouldRetrySenderTillThresholdNotRecovered(queueConsumer.shouldRetrySenderTillThresholdNotRecovered())
				.setShouldThrowExceptionPostRetry(queueConsumer.shouldThrowExceptionPostRetry())
				.setNumberOfRetriesToWait(queueConsumer.numberOfRetriesBeforeThrowingExceptionWhenThreshold())
				.setShouldThrowExceptionWhenThresholdAtAdd(queueConsumer.shouldThrowExceptionWhenThresholdAtAdd())
				.setThresholdWhenNoMoreItemsShouldBeHandled(queueConsumer.messagesCountThreshold())
				.setWaitForRetryThresholdLimit(queueConsumer.sleepTimeBetweenRetriesWhenThreshold())
				;
			if(batch) {
				dt.setMaxBatchConsumer(queueConsumer.maxConsumers())
				.setMinBatchConsumer(queueConsumer.minConsumers());
			} else {
				dt.setMaxDirectConsumer(queueConsumer.maxConsumers())
				.setMinDirectConsumer(queueConsumer.minConsumers());
			}
			
		}
	}

	/**
	 * @param objectHavingMethod
	 * @param method
	 * @param batch
	 * @param queueName
	 * @param numberOfConsumers
	 * @param batchSize
	 */
	protected void initializeQueueConsumer(Object objectHavingMethod, Method method, boolean batch, String queueName,
			int numberOfConsumers, int batchSize) {
		if ((batch && manager.getBatchConsumerCount(queueName) > 0) || ( !batch && manager.getDirectConsumerCount(queueName) > 0)) {
			logger.info("Already registerd for this type of queue. So skipping.");
				return;
		}
		java.util.function.Consumer consumer = t-> {
			try {
				method.invoke(objectHavingMethod, t);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
		if (batch) {
			manager.initializeForBatch(queueName, consumer, numberOfConsumers);
			manager.setBatchSize(queueName, batchSize);
		} else {
			manager.initialize(queueName, consumer, numberOfConsumers);
		}
	}
}
