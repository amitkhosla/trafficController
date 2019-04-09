/**
 * 
 */
package org.ak.trafficController.multiRequests;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Control flow of multiple requests for a given method.
 * @author amit.khosla
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ControlMultiRequest {
	
	/**
	 * Unique name which will denote a method.
	 * If not passed, we will prepare name from the join point.
	 * @return Unique name
	 */
	String uniqueName() default "";
	
	/**
	 * This means, we would like to let already running results keep running in their own thread and we will not wait for that.
	 * @return true if we want to wait for already running task for the given name and param
	 */
	boolean shouldWait() default true;
	
	/**
	 * This should be set as true where input is of type list and output is of type Map; where we want to find results from already run
	 * and also from running and remaining are run which later are joined. 
	 * @return true if we have to consider partial results
	 */
	boolean shouldConsiderPartial() default false;
	
	
	/**
	 * Once run, we can reuse the results for given time. We need to be extra careful that we should choose optimum value 
	 * else we can have high amount of memory being used in results.
	 * The value is in seconds.
	 * @return number of seconds for which we are looking to keep results 
	 */
	long reusePreviousRunResult() default 1; /// TODO- We need to work on memory management as well like deleting(LRU etc) a few records in case of issue
	
	/**
	 * Number of retries if any required for this operation in case we face some exception.
	 * @return Number of retries
	 */
	int numberOfRetries() default 0;
	
	/**
	 * Time in millisecond before retrying.
	 * @return interval in retry
	 */
	int retryInterval() default 0;
	
	/**
	 * Unique Name finder method for the given method.
	 * @return Unique name finder method in the same class
	 */
	String nameFinderMethod() default ""; 
}
