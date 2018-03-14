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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.messaging.annotations.Consumer;
import org.ak.trafficController.messaging.annotations.Queued;
import org.ak.trafficController.messaging.mem.DynamicSettings;
import org.ak.trafficController.messaging.mem.InMemoryQueueManager;
import org.ak.trafficController.messaging.mem.InMemoryQueueTuner;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

/**
 * Handler which handles different queue annotations.
 * {@link Queued} is used to generate message and put in specified queue.
 * {@link Consumer} is used to register consumer.
 * @author amit.khosla
 *
 */
@Aspect
@Named
public class QueueAnnotationsHandler {
	
	/**
	 * Context to find the class from spring context.
	 */
	@Inject
	ApplicationContext context;
	
	private static final String RETURN = "RETURN";

	private static final String CONSUMER = "CONSUMER";

	Logger logger = Logger.getLogger(QueueAnnotationsHandler.class.getName());
	
	/**
	 * Manager to map queues.
	 */
	InMemoryQueueManager manager = new InMemoryQueueManager();
	
	/**
	 * Tuner to tune the queues which are enabled for dynamic settings.
	 */
	InMemoryQueueTuner tuner = new InMemoryQueueTuner(manager);
	
	/**
	 * Sleep time after which it will be tried to adjust.
	 */
	long sleepInterval = 1000L;

	/**
	 * Initialize the tuner.
	 */
	@PostConstruct
	public void init() {
		tuner.setSleepInterval(sleepInterval).startTuning();
	}
	
	/**
	 * Shutdown all queues created and also shutdown tuner.
	 * This will be run while unloading of this object, which is most likely while shutdown of application.
	 */
	@PreDestroy
	public void shutdown() {
		tuner.shutdown();
		manager.shutdown();
	}

	/**
	 * Queues name mappings to save again finding the name from join point.
	 */
	Map<Method, String> queueNameMapping = new ConcurrentHashMap<>();
	/**
	 * Does a method already been processed as Queued annotation?
	 */
	Map<Method, Boolean> queueMethodsProcessedForConsumer = new ConcurrentHashMap<>();
	/**
	 * Does a method already been processed as Consumer annotation?
	 */
	Map<Method, Boolean> consumeMethodsProcessed = new ConcurrentHashMap<>();
	

	/**
	 * This method handles {@link Queued} annotated methods.
	 * This method runs the annotated method, captures its output, put it in queue and return back the output to user.
	 * @param joinPoint Join point
	 * @param queued Queued object
	 * @return Returns the output of the annotated method
	 * @throws Throwable Any exception occurred while execution.
	 */
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

	/**
	 * Process queue consumer if configured to.
	 * @param queued Queued object
	 * @param joinPoint Join point
	 * @param obj Object which is sent to queue, to find the method in class
	 * @param queueName Name of queue
	 * @throws InstantiationException if no bean present in application context and we cannot create instance
	 * @throws IllegalAccessException If method is not found
	 */
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
	 * Register consumer for queued data.
	 * @param queued Queued 
	 * @param queueName Name of queue
	 * @param method Method on which Queued is annotated
	 * @param actualMethod The method which is acting as consumer
	 * @throws InstantiationException if no bean present in application context and we cannot create instance
	 * @throws IllegalAccessException If method is not found
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
	 * Get actual consumer method.
	 * @param queued Queued annotation
	 * @param obj Object to find method
	 * @param signature Signature of annotated method
	 * @param method Annotated method
	 * @return Method eligible to be consumer
	 */
	protected Method getActualConsumerMethod(Queued queued, Object obj, MethodSignature signature, Method method) {
		List<Method> matchingMethods = getProbableMethods(queued);
	
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

	/**
	 * Get appropriate method for given class and list of methods.
	 * @param obj Class object
	 * @param matchingMethods Matching methods list (methods having same name)
	 * @param directConsumer Is it a direct consumer or batch
	 * @return Method eligible to act as consumer
	 */
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

	/**
	 * Is of type required?
	 * @param cls Class to be scanned
	 * @param m Method to be scanned
	 * @return Is of type required collection
	 */
	protected boolean isOfTypeRequiredCollection(Class cls, Method m) {
		return getGenericTypeFromParam(m).isAssignableFrom(cls);
	}

	/**
	 * Get object for the given consumer class. This will be tried from spring context, if not found, a new object will be tried.
	 * @param consumerClass Consumer class of which we need object
	 * @return Object of the class
	 * @throws InstantiationException In case instantiation failed for case where context is not having bean for given class
	 * @throws IllegalAccessException In case instantiation failed for case where context is not having bean for given class
	 */
	protected Object getObjectHavingMethod(Class consumerClass) throws InstantiationException, IllegalAccessException {
		Map<String, Object> beansPresent = context.getBeansOfType(consumerClass);
		if (beansPresent.size() > 0) {
			return beansPresent.values().iterator().next();
		}
		return consumerClass.newInstance();
	}

	/**
	 * Is method of type required?
	 * @param cls Class of type for which we need to verify
	 * @param m Method which is to be scanned
	 * @return true if succeed in matching
	 */
	protected boolean isOfTypeRequired(Class cls, Method m) {
		return m.getParameterTypes()[0].isAssignableFrom(cls);
	}

	/**
	 * Get probable methods which are matching the name specified in queued as consumer method.
	 * @param queued Queued
	 * @return List of probable methods
	 */
	protected List<Method> getProbableMethods(Queued queued) {
		List<Method> matchingMethods = new ArrayList<>();
		Method[] methods = queued.consumerClass().getDeclaredMethods();
		for (Method m : methods) {
			if (m.getName().equals(queued.consumerMethod()) && m.getParameterTypes().length == 1) {
				matchingMethods.add(m);
			}
		}
		return matchingMethods;
	}

	/**
	 * Get queue name. If name is passed, the same is returned. In case name is not passed, it is created from join point.
	 * For creation, producer's return type and consumer's parameter type is identified.
	 * Also if the batch processing (batch put or batch consumer) type is identified from the type of collection.
	 * @param name Queue name as passed in Queued or consumer
	 * @param joinPoint Join point of the caller method
	 * @param shouldVerifyBatch Batch?
	 * @param producerOrConsumer Producer or consumer?
	 * @return name of queue
	 */
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
	 * Get type of annotated method. If producer them return type is verified else first param is verified.
	 * @param shouldVerifyBatch batch?
	 * @param producerOrConsumer producer or consumer?
	 * @param signature Signature of method
	 * @param method Annotated Method
	 * @return Type of data
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

	/**
	 * Check if consumer or producer is of primitive type. In that case the name will be of wrapper.
	 * @param returnType Type to be verified
	 * @return Name of wrapper type or original if non primitive
	 */
	protected String checkPrimitive(Class returnType) {
		String output;
		if (returnType.isPrimitive()) {
			output = getWrapperForPrimitive(returnType.getName());
		} else {
			output = returnType.getName();
		}
		return output;
	}

	/**
	 * Get type of collection.
	 * @param producerOrConsumer Producer or consumer?
	 * @param method Annotated method
	 * @return Type of data in collection
	 */
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

	/**
	 * Get generic type for given consumer.
	 * @param method Consumer method
	 * @return Generic type
	 */
	protected Class getGenericTypeFromParam(Method method) {
		Class returnType;
		ParameterizedType type = (ParameterizedType) method.getParameters()[0].getParameterizedType();
		returnType =  (Class)type.getActualTypeArguments()[0];
		return returnType;
	}

	/**
	 * Get type of direct flow.
	 * @param producerOrConsumer Producer or consumer?
	 * @param signature Method signature of annotated method
	 * @return Type of data
	 */
	protected Class getTypeForDirectFlow(String producerOrConsumer, MethodSignature signature) {
		Class returnType;
		if (RETURN.equalsIgnoreCase(producerOrConsumer)) {
			returnType = signature.getReturnType();
		} else {
			returnType = signature.getParameterTypes()[0];
		}
		return returnType;
	}
	
	/**
	 * Wrappers mapping.
	 */
	static Map<String, Class> wrappersMapping = new HashMap<>();
	static {
		wrappersMapping.put("int", Integer.class);
		wrappersMapping.put("float", Float.class);
		wrappersMapping.put("double", Double.class);
		wrappersMapping.put("long", Long.class);
		wrappersMapping.put("boolean", Boolean.class);
	}
	
	/**
	 * Get wrapper of primitive.
	 * @param name primitive type
	 * @return Wrapper name
	 */
	protected String getWrapperForPrimitive(String name) {
		Class class1 = wrappersMapping.get(name);
		return class1 != null ? class1.getName() : null;
	}

	/**
	 * This method handles {@link Consumer} annotated flows.
	 * This method on call will register a consumer (if not already registered) and return the value to caller.
	 * @param joinPoint Join point of the caller method
	 * @param queueConsumer Consumer
	 * @return Output of the method
	 * @throws Throwable Throws in case of failure in processing
	 */
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

	/**
	 * Initialize queue for given consumer.
	 * @param queueConsumer Queue consumer Annotation object
	 * @param objectHavingMethod Object to be used for running method
	 * @param method Method to be run as consumer of queue
	 * @param batch Is batch?
	 * @param queueName Name of queue
	 */
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
	 * Sets dynamic nature if configured.
	 * @param queueConsumer Object of annotation Consumer
	 * @param batch Is batch?
	 * @param queueName Name of queue
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
	 * Initialize the queue consumer.
	 * @param objectHavingMethod  Object to be used to call consume data in queue
	 * @param method Method to be used to consumer data in queue
	 * @param batch Is a batch consumer?
	 * @param queueName Name of queue
	 * @param numberOfConsumers Number of consumers
	 * @param batchSize Size of batch in case of batch consumer
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
