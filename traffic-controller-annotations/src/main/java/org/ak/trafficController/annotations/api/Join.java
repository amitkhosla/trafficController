package org.ak.trafficController.annotations.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation to be used with {@link Parallel}. This annotation will join all the data created by parallel processing of items.
 * All {@link Controlled} annotated methods if called from {@link Parallel} will be executed in parallel and store their data. 
 * The data thus generated will be joined using the annotated method.
 * The input to the annotated method will be in exact order of how it is defined in Parallel annotated method.
 * @author amit.khosla
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Join {
	/**
	 * This if set will be called before creating task chain to retrieve the data which will be used in all tasks.
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Join#threadDetailsDataExtractClass()} and {@link Join#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Join#threadDetailsProcessorClass()} and {@link Join#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Join#threadDetailsCleanerClass()} and {@link Join#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Class name of data extractor which will be used by processors and cleaners
	 */
	Class threadDetailsDataExtractClass() default Join.class;
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Join#threadDetailsDataExtractClass()} and {@link Join#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Join#threadDetailsProcessorClass()} and {@link Join#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Join#threadDetailsCleanerClass()} and {@link Join#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Method name of data extractor which will be used by processor and cleaner
	 */
	String threadDetailsDataExtractMethodName() default "";
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Join#threadDetailsDataExtractClass()} and {@link Join#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Join#threadDetailsProcessorClass()} and {@link Join#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Join#threadDetailsCleanerClass()} and {@link Join#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Class name of processor which will run before each task
	 */
	Class threadDetailsProcessorClass() default Join.class;
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Join#threadDetailsDataExtractClass()} and {@link Join#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Join#threadDetailsProcessorClass()} and {@link Join#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Join#threadDetailsCleanerClass()} and {@link Join#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Method name of processor which will run before each task
	 */
	String threadDetailsProcessorMethodName() default "";
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Join#threadDetailsDataExtractClass()} and {@link Join#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Join#threadDetailsProcessorClass()} and {@link Join#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Join#threadDetailsCleanerClass()} and {@link Join#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Class name of cleaner which will run post each task
	 */
	Class threadDetailsCleanerClass() default Join.class;
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Join#threadDetailsDataExtractClass()} and {@link Join#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Join#threadDetailsProcessorClass()} and {@link Join#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Join#threadDetailsCleanerClass()} and {@link Join#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Method name of cleaner which will run post each task
	 */
	String threadDetailsCleanerMethodName() default "";
}
