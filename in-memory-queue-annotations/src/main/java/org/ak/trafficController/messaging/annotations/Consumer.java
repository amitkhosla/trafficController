package org.ak.trafficController.messaging.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotated method will act as consumer of the queue being populated by @Queued annotated method.
 * The name of queue determines the number of queue.
 * If queue name is not passed the queue is considered to be of type of parameter the annotated method. 
 * The annotated method ideally should be having single parameter of the type required.
 * If more than one params are present in method, type check is done and if found multiple types, then first element of type is used.
 * @author Amit Khosla
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Consumer {
	/**
	 * Name of consumer. If passed, the consumer is bound to the queue with name. 
	 * @return Name of consumer
	 */
	String name() default "";
	
	/**
	 * Number of threads you want to consume this data.
	 * For example, this consumer is performing some I/O operation and you want anything in queue is picked by 5 threads doing the same work.
	 * @return Number of consumers
	 */
	int numberOfConsumers() default 1;

	
	/**
	 * At times, you might require some dynamic nature like if I have so many items in queue, I should exceed the number of consumers 
	 * and when it cool off the extra thread can be decommissioned.
	 * Or you want your queue to stop accepting anything if already having a lot of data.
	 * Or if the data is not much important, you want to flush all in case you might have some n/w glitch 
	 * because of which you cannot connect to some cache server, so you may want to flush all.
	 * 
	 * Configuration is set by the other params of this annotation.
	 * 
	 * @return Dynamic nature
	 */
	boolean dynamicNature() default false;
	
	/**
	 * Max number of consumers that can be allowed. 
	 * @return Max number of consumers
	 */
	int maxConsumers() default 5;
	
	
	/**
	 * Minimum number of consumers that is required to process queue in case the queue is having very low number of messages.
	 * @return Minimum number of queue
	 */
	int minConsumers() default 1;
	
	/**
	 * Number of messages present in queue when a new thread for the queue is created.
	 * Whenever the messages present in queue reaches this threshold, a new consumer is tried.
	 * @return Threshold of messages when increasing queue
	 */
	long numberOfMessagesInQueueWhenNewConsumerShouldBeCreated() default 10000;
	
	/**
	 * When the heavy load is over and the consumption rate is more than production rate, soon we find that we can reduce our consumption rate.
	 * So, this parameter tells us that now the number of messages is reduced to that level where we can reduce number of consumers. 
	 * @return Number of messages in queue when we want to reduce consumers
	 */
	long numberOfMessagesInQueueWhenShouldTryToReduceConsumers() default 100;
	
	/**
	 * 
	 * @return
	 */
	int maxNumberOfMessagesWhenExceptionOrSomeCleanupShouldStart() default Integer.MAX_VALUE;
	
	
	/**
	 * @return
	 */
	boolean shouldThrowExceptionWhenHighLimitReach() default false;


	boolean shouldStopAddingAtThreshold() default false;


	boolean shoulClearOnThreshold() default false;


	boolean shouldRetrySenderTillThresholdNotRecovered() default false;


	boolean shouldThrowExceptionPostRetry() default false;


	long numberOfRetriesBeforeThrowingExceptionWhenThreshold() default 5l;

	long sleepTimeBetweenRetriesWhenThreshold() default 50l;
	
	boolean shouldThrowExceptionWhenThresholdAtAdd() default false;

	long messagesCountThreshold() default 10000;

	boolean batch() default false;


	//boolean shouldPauseSenderTillThresholdNotRecovered() default false;
}
