package org.ak.trafficController.annotations.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import static org.ak.trafficController.annotations.api.Constants.*;

/**
 * This annotation is to be used when you want your method to be executed in parallel.
 * Also max number of consumers working for this method is also configurable by setting below params.
 * @author Amit Khosla
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Submit {

	/**
	 * Executor Name which will map the execution of the method.
	 * If not passed and no other value set will default to DEFAULT.
	 * You can also set it to be "Integer, Integer" like "10,5" 
	 * which will mean that the executor you want is 10 normal consumers and 5 slow consumers.
	 * Also you can set it like "Integer%, Integer%" like "70%, 200%".
	 * This will mean you want 70% of number of cores as normal and double of number of cores as slow tasks.
	 * In the end the string passed will be the key and can be used in multiple methods to use same pool.
	 * If not passed anything and consumers set then a new executor will be assigned based on name of method and class.
	 * @return Executor Name
	 */
	String executorName() default "";
	
	/**
	 * This is the max consumer you want this method to be processed with.
	 * If added executor name this will be mapped to executorName attribute.
	 * Also if we are adding same executorName at multiple places, first setting will be honored.
	 * If executorName is not passed and this value is set, a new executor for the annotated method will be set.
	 * But this will imply to all methods overloaded for this given method.
	 * You can either give a numeric value or method name which return integer value for consumer.
	 * @return max consumers for the method. 
	 */
	String maxConsumer() default ZERO;
	
	/**
	 * This is applicable for task type Slow.
	 * Idea is the for slow tasks like IO, you can have separate threads which do not block you main tasks.
	 * It is similar to maxConsumer. You can either pass method name or integer value.
	 * Both attributes: max consumers and max slow consumers can be mapped to a specific name to keep max consumers in control.
	 * @return max consumer for slow tasks.
	 */
	String maxSlowConsumer() default ZERO;
	
	/**
	 * This defines which type of task is this. 
	 * If this method is for IO operations like DB call or any file operation etc, it should be slow.
	 * If this method is for normal calculations etc, you can keep it NORMAL.
	 * @return Task type.
	 */
	TaskType taskType() default TaskType.NORMAL;
	
	/**
	 * This if set will be called before creating task chain to retrieve the data which will be used in all tasks.
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Submit#threadDetailsDataExtractClass()} and {@link Submit#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Submit#threadDetailsProcessorClass()} and {@link Submit#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Submit#threadDetailsCleanerClass()} and {@link Submit#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Class name of data extractor which will be used by processors and cleaners
	 */
	Class threadDetailsDataExtractClass() default Submit.class;
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Submit#threadDetailsDataExtractClass()} and {@link Submit#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Submit#threadDetailsProcessorClass()} and {@link Submit#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Submit#threadDetailsCleanerClass()} and {@link Submit#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Method name of data extractor which will be used by processor and cleaner
	 */
	String threadDetailsDataExtractMethodName() default "";
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Submit#threadDetailsDataExtractClass()} and {@link Submit#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Submit#threadDetailsProcessorClass()} and {@link Submit#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Submit#threadDetailsCleanerClass()} and {@link Submit#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Class name of processor which will run before each task
	 */
	Class threadDetailsProcessorClass() default Submit.class;
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Submit#threadDetailsDataExtractClass()} and {@link Submit#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Submit#threadDetailsProcessorClass()} and {@link Submit#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Submit#threadDetailsCleanerClass()} and {@link Submit#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Method name of processor which will run before each task
	 */
	String threadDetailsProcessorMethodName() default "";
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Submit#threadDetailsDataExtractClass()} and {@link Submit#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Submit#threadDetailsProcessorClass()} and {@link Submit#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Submit#threadDetailsCleanerClass()} and {@link Submit#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Class name of cleaner which will run post each task
	 */
	Class threadDetailsCleanerClass() default Submit.class;
	
	/**
	 * As name suggests, this is for handling thread local issues. As the tasks run in different thread so they loose their thread local.
	 * So, if any thread local is required, this set of items need to be set. 
	 * {@link Submit#threadDetailsDataExtractClass()} and {@link Submit#threadDetailsDataExtractMethodName()} are used to retrieve data from main thread.
	 * This means that method specified in specified class will be executed and stored to be used in each thread.
	 * To set the thread local, we need to process this data using {@link Submit#threadDetailsProcessorClass()} and {@link Submit#threadDetailsProcessorMethodName()}.
	 * This will be run before running any task. This method should be expecting the data stored in main thread. The method is responsible for setting thread locals.
	 * Post running task, we also need to clear the thread locals for which method specified in {@link Submit#threadDetailsCleanerClass()} and {@link Submit#threadDetailsCleanerMethodName()}.
	 * Cleaners will run post running each task.  Cleaner method is also expected to use the object created in data extractor.
	 * @return Method name of cleaner which will run post each task
	 */
	String threadDetailsCleanerMethodName() default "";
}
