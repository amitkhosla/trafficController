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
	String name() default "";

	boolean itemInCollection() default false;
}
