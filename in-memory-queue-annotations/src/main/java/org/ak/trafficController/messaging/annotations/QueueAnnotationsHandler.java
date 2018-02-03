package org.ak.trafficController.messaging.annotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

import org.ak.trafficController.messaging.mem.DynamicSettings;
import org.ak.trafficController.messaging.mem.InMemoryQueueManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
@Named
public class QueueAnnotationsHandler {
	
	Logger logger = Logger.getLogger(QueueAnnotationsHandler.class.getName());
	
	InMemoryQueueManager manager = new InMemoryQueueManager();
	

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
			String queueName = getQueueName(queued.name(), joinPoint);
			if (queued.itemInCollection()) {
				manager.addItems(queueName, (Collection) obj);
			} else {
				manager.add(queueName, obj);
			}
		}
		return obj;
	}

	protected String getQueueName(String name, ProceedingJoinPoint joinPoint) {
		return name;
	}
	
	@Around("execution(@org.ak.trafficController.messaging.annotations.Consumer * *(..)) && @annotation(queueConsumer)")
	public Object consume(ProceedingJoinPoint joinPoint, Consumer queueConsumer) throws Throwable {
		//System.out.println("called2....");
		Object objectHavingMethod = joinPoint.getTarget();
		MethodSignature signature = ((MethodSignature) joinPoint.getSignature());
		Method method = signature.getMethod();
		java.util.function.Consumer consumer = t-> {
			try {
				method.invoke(objectHavingMethod, t);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
		String queueName = getQueueName(queueConsumer.name(), joinPoint);
		if (queueConsumer.batch()) {
			manager.initializeForBatch(queueName, consumer, queueConsumer.numberOfConsumers());
		} else {
			manager.initialize(queueName, consumer, queueConsumer.numberOfConsumers());
		}
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
			if(queueConsumer.batch()) {
				dt.setMaxBatchConsumer(queueConsumer.maxConsumers())
				.setMinBatchConsumer(queueConsumer.minConsumers());
			} else {
				dt.setMaxDirectConsumer(queueConsumer.maxConsumers())
				.setMinDirectConsumer(queueConsumer.minConsumers());
			}
			
		}
		return joinPoint.proceed();
	}
}
