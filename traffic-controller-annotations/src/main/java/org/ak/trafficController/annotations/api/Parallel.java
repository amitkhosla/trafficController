package org.ak.trafficController.annotations.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation to be used with {@link Parallel}. This annotation will join all the data created by parallel processing of items.
 * All {@link Controlled} annotated methods if called from {@link Parallel} will be executed in parallel and store their data. 
 * The data thus generated will be joined using the annotated method.
 * The input to the annotated method will be in exact order of how it is defined in Parallel annotated method.
 * If no joining is required, we do not need to add a method having join.
 * Without a joiner, Parallel annotated method will return null.
 * @author amit.khosla
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Parallel {
	/**
	 * This defines the time for which this parallel task execution should be waited for before declaring it not working and throwing exception.
	 * 1,800,000 milliseconds (30 minutes) is default value. 
	 * @return wait time in milliseconds
	 */
	int waitTimeInMilliSeconds() default 30*60*1000;
}
