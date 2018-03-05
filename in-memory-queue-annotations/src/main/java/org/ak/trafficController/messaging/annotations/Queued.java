package org.ak.trafficController.messaging.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation can be used to queue output of annotated method.
 * If no name is passed, it is queued for class type.
 * @author Amit Khosla
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Queued {
	/**
	 * Name of the queue. If not specified, will be taken as type of the queue. 
	 * For example if this consumer is of type Person, the name will be full name of class Person.
	 * @return Name of queue
	 */
	String name() default "";

	/**
	 * Is this method returning a collection and do we want to have items in this collection as item in queue?
	 * This means if a method is returning a collection of Employees and we want the queue to contain employee as a data. 
	 * If this is set as true, all employees will be added to queue and consumer will consumer these individually.
	 * @return Item in collection
	 */
	boolean itemInCollection() default false;
	
	/**
	 * Consumer class which should be used as consumer of messages produced by this.
	 * This will register the method specified in {@link Queued#consumerMethod()} as consumer of this message.
	 * This is not required if the consumer is registered by calling it directly.
	 * @return Class of consumer
	 */
	Class consumerClass() default Queued.class;
	
	/**
	 * Consumer method which should be used as consumer of messages produced by this.
	 * This will register this method specified in {@link Queued#consumerClass()} as consumer of this message.
	 * This is not required if the consumer is registered by calling it directly.
	 * @return Consumer method
	 */
	String consumerMethod() default "";
	
	/**
	 * This is to define if the specified consumer in {@link Queued#consumerClass()} class's method {@link Queued#consumerMethod()} is a batch consumer.
	 * @return Is consumer a batch consumer
	 */
	boolean listConsumer() default false;
}
