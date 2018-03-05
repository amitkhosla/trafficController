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
	 * If the threshold of tooo much data is reached, should we throw exception to producer till we do not recover.
	 * If this configured, no retries will be performed.
	 * @return Should throw exception 
	 */
	boolean shouldThrowExceptionWhenHighLimitReach() default false;


	/**
	 * Should addition is stopped at threshold. This means the messages will be lost. So, recommended only in cases where messages can be lost.
	 * @return
	 */
	boolean shouldStopAddingAtThreshold() default false;


	/**
	 * Should we clear the queue in case threshold is reached.
	 * @return Should clear threshold
	 */
	boolean shoulClearOnThreshold() default false;


	/**
	 * Should retry for few times before failing to add data in queue.
	 * @return should retry sender till threshold not recovered
	 */
	boolean shouldRetrySenderTillThresholdNotRecovered() default false;


	/**
	 * Should throw exception if all retries are over at producer side.
	 * @return Should throw exception post retry
	 */
	boolean shouldThrowExceptionPostRetry() default false;


	/**
	 * Number of retries before throwing exception.
	 * @return Number of retries
	 */
	long numberOfRetriesBeforeThrowingExceptionWhenThreshold() default 500l;

	/**
	 * Sleep time in milliseconds before trying next sleep time.
	 * @return Sleep time between retries
	 */
	long sleepTimeBetweenRetriesWhenThreshold() default 50l;
	
	/**
	 * Should throw exception when threshold to add.
	 * @return Should throw exception when threshold is reached
	 */
	boolean shouldThrowExceptionWhenThresholdAtAdd() default false;

	/**
	 * In case the load is too high and we want our system to remain safe, we can opt for this to start cleanup or throw exception to producer that they cannot add more data. 
	 * @return Messages count when we say that data is too much
	 */
	long messagesCountThreshold() default 1_00_000;

	/**
	 * Is it a batch consumer? Batch consumer is one which accept the items in list.
	 * @return Is it a batch consumer?
	 */
	boolean batch() default false;

	/**
	 * For any batch consumer this is the maximum number of items that will be consumed in one go.
	 * @return Batch size
	 */
	int batchSize() default 10;


	//boolean shouldPauseSenderTillThresholdNotRecovered() default false;
}
