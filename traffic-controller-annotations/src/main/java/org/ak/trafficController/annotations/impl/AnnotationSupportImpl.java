package org.ak.trafficController.annotations.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.ExecutableTask;
import org.ak.trafficController.ParallelExecutingTask;
import org.ak.trafficController.ParallelTask;
import org.ak.trafficController.RunnableToBeExecuted;
import org.ak.trafficController.Task;
import org.ak.trafficController.TaskExecutor;
import org.ak.trafficController.UnlinkedTask;
import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Join;
import org.ak.trafficController.annotations.api.Parallel;
import org.ak.trafficController.annotations.api.Submit;
import org.ak.trafficController.annotations.api.TaskType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;


/**
 * This class handles task handling annotations (Controlled, Submit, Parallel, Join).
 * Controlled if annotated, in normal flow will run in specified executor. This helps in throttling as only specified number of tasks can run at a time.
 * Submit if annotated, method is submitted to task executor will be run async without and won't make calling thread to wait for completion of logic.
 * Parallel if annotated, all method calls to Controlled annotated methods is made in parallel. Join annotated method joins back the the result created by controlled annotated methods.
 * @author amit.khosla
 */
@Aspect
@Named
public class AnnotationSupportImpl {

	Logger logger = Logger.getLogger(AnnotationSupportImpl.class.getName());
	
	/**
	 * Parallel id
	 */
	AtomicInteger parallelId = new AtomicInteger(0);
	
	@Inject
	TaskHelper taskHelper;
	
	@Inject
	ParallelJoinHelper parallelJoinHelper;
	
	/**
	 * Handles Parallel annotated methods. 
	 * Parallel if annotated, all method calls to Controlled annotated methods is made in parallel. Join annotated method joins back the the result created by controlled annotated methods.
	 * It first execute the annotated method and creates task chain to be run in parallel followed by a joiner method which will join these results.
	 * Parallel should be used only where we are calling different methods annotated with controlled and in different class to allow AOP play its work.
	 * Output returned will be the output from Join operation or the last operation.
	 * @param joinPoint Join point
	 * @param parallel Parallel
	 * @return Return output of the operation
	 * @throws Throwable In case of any exception in processing
	 */
	@Around("execution(@org.ak.trafficController.annotations.api.Parallel * *(..)) && @annotation(parallel)")
	 public Object runParallel(ProceedingJoinPoint joinPoint, Parallel parallel) throws Throwable {
		int currentParallelId = parallelId.incrementAndGet();
		parallelJoinHelper.map.put(currentParallelId, new ConcurrentHashMap<>());
		AtomicInteger taskId = new AtomicInteger(0);
		AtomicInteger earlierParallelTaskId = new AtomicInteger(0);
		Task originalTask = parallelJoinHelper.getTask();
		
		boolean isSubParallelTask = originalTask != null;
		if (isSubParallelTask) {
			//this task is one task of parallel tasks.
			taskId.set(parallelJoinHelper.getObjectKeyForParalleldTask());
			earlierParallelTaskId.set(parallelJoinHelper.getParallelId());
		}
		ParallelJoinHelper.setParallelTaskId(currentParallelId);	
		//if (!isSubParallelTask) {
		ExecutableTask thisParallelTask = TaskExecutor.getInstance().of(()->{});
		thisParallelTask.setName("ParallelTask" + currentParallelId);
		ParallelExecutingTask<Object> thenParallel = thisParallelTask.thenParallel(()->{});
		thenParallel.setName("ParallelTaskName:" + currentParallelId);
		parallelJoinHelper.setTask(thenParallel); //set dummy task
		//}
		if (!isSubParallelTask) {
			Object val = directParallelTaskHandling(joinPoint, currentParallelId, parallel);
			return val;
		} else {
			joinPoint.proceed();
			if (originalTask instanceof ParallelTask) {
				subProcessHandling(currentParallelId, taskId, earlierParallelTaskId, originalTask, thisParallelTask);
			}
			return null;
		}
	}

	/**
	 * Sub process handling. This method will be called while handling Parallel in case where parallel is being called inside another parallel flow.
	 * @param currentParallelId Current parallel id
	 * @param taskId Task id
	 * @param earlierParallelTaskId Earlier parallel task id
	 * @param originalTask Original task
	 * @param thisParallelTask Current parallel task
	 */
	protected void subProcessHandling(int currentParallelId, AtomicInteger taskId, AtomicInteger earlierParallelTaskId,
			Task originalTask, ExecutableTask thisParallelTask) {
		Task task = parallelJoinHelper.getTask().then(()->{
			Object result = performCleanup(currentParallelId);
			if (Objects.nonNull(result)) {
				ParallelJoinHelper.putObject(earlierParallelTaskId.get(), taskId.get(), result);
			}
		});
		((ParallelTask) originalTask).addTask(thisParallelTask);
		parallelJoinHelper.setTask(originalTask);
		ParallelJoinHelper.removeParallelId(currentParallelId);
		ParallelJoinHelper.setParallelTaskId(earlierParallelTaskId.get());
	}

	/**
	 * Direct process handling. This method will be called while handling parallel in case where it is not called from another parallel flow.
	 * @param joinPoint Join point
	 * @param currentParallelId Current parallel id
	 * @param parallel Parallel
	 * @return Value of the tasks execution
	 * @throws Throwable In case of any issue in processing
	 */
	protected Object directParallelTaskHandling(ProceedingJoinPoint joinPoint, int currentParallelId, Parallel parallel) throws Throwable {
		AtomicReference<Object> output = new AtomicReference<Object>(joinPoint.proceed());
		Task cleanUpTask = parallelJoinHelper.getTask().then(()->{
			output.set(performCleanup(currentParallelId));
		});
		cleanUpTask.setName("Clean up task " + currentParallelId);
		parallelJoinHelper.setTask(cleanUpTask);
		//if (((MethodSignature) joinPoint.getSignature()).
		ParallelJoinHelper.taskChain.get().start(parallel.waitTimeInMilliSeconds());
		parallelJoinHelper.removeTask();
		Object val = output.get();
		if (val != null && val.getClass() == JoinResult.class) {
			val = ((JoinResult) val).result;
		}
		return val;
	}
	
	/**
	 * Cleanup of parallel process and return the output.
	 * @param currentParallelId Current parallel id for which we need cleanup and value
	 * @return Data created by join task or last controlled task
	 */
	protected Object performCleanup(int currentParallelId) {
		Map<Integer, Object> map = parallelJoinHelper.map.get(currentParallelId);
		Object obj = map.get((map.size()));
		parallelJoinHelper.map.remove(parallelId);
		return obj;
	}

	/**
	 * Handles Async operation where user is looking to submit something to get processed but calling thread should not wait for it.
	 * If called in Parallel flow, will be added as a async task/
	 * @param joinPoint Join point
	 * @param async Async
	 * @return Returns null if no exception
	 * @throws Throwable In case of exception in execution of annotated method
	 */
	@Around("execution(@org.ak.trafficController.annotations.api.Submit * *(..)) && @annotation(async)")
	 public Object runAsync(ProceedingJoinPoint joinPoint, Submit async) throws Throwable {
		
		RunnableToBeExecuted taskToWorkOn = ()-> {
			try{
				joinPoint.proceed();	
			} catch (Throwable e) {
				logger.log(java.util.logging.Level.WARNING, "exception occured while running a submit request", e);
			}
		};
		TaskExecutor taskExecutor = taskHelper.getTaskExecutor(async, joinPoint);
		Task task = null;
		TaskType taskType = async.taskType();
		switch (taskType) {
		case NORMAL: 
			task = taskExecutor.of(taskToWorkOn);
			break;
		case SLOW :
			task = taskExecutor.slowOf(taskToWorkOn);
		}
		Task taskInThread = ParallelJoinHelper.getTask();
		if (taskInThread == null) {
			task.submit();
		} else {
			AtomicReference<Task> taskReference = new AtomicReference<Task>(task);
			((ParallelTask) taskInThread).addRunnables(convertAnnotationTaskTypeToFrameworkTaskType(taskType), taskExecutor, ()->taskExecutor.enque(taskReference.get()));
		}
		return null;
	}

	/**
	 * Get core task type for given annotations task type.
	 * @param taskType Task type
	 * @return Core task type
	 */
	protected org.ak.trafficController.Task.TaskType convertAnnotationTaskTypeToFrameworkTaskType(TaskType taskType) {
		return taskType == TaskType.NORMAL ? org.ak.trafficController.Task.TaskType.NORMAL : org.ak.trafficController.Task.TaskType.SLOW;
	}

	/**
	 * Handles Join operation.
	 * Annotated method will be called with exactly same number of attributes for which non null data was returned by controlled methods.
	 * If we have 4 methods defined and out of which 2 methods returned some value, join annotated method is expected to expect only these two methods in the same sequence as they would have called sequentially.
	 * @param joinPoint Join point
	 * @param join Join
	 * @return This method will return null but will set output against its id in parallel map
	 * @throws Throwable In case there is issue in processing
	 */
	@Around("execution(@org.ak.trafficController.annotations.api.Join * *(..)) && @annotation(join)")
	public Object runJoin(ProceedingJoinPoint joinPoint, Join join) throws Throwable {
		int taskId = parallelJoinHelper.getObjectKeyForParalleldTask();
		int parallelTaskId = ParallelJoinHelper.getParallelId();
		AtomicReference<Object> output = new AtomicReference<Object>(null);
		Task joinerTask = parallelJoinHelper.getTask().then(()->{
			List<Object> list = new ArrayList<>();
			addAllResultObjectsTillNowInList(list, parallelTaskId, taskId);
			if (!list.isEmpty()) {
				output.set(joinPoint.proceed(getObjectArrayFromList(list)));
			} else {
				output.set(joinPoint.proceed());
			}
			JoinResult jr = new JoinResult();
			jr.result = output.get(); 
			ParallelJoinHelper.map.get(parallelTaskId).put(taskId, jr);
		});
		joinerTask.setName("joiner ParallelId:" + parallelTaskId + " taskId : " + taskId + getTaskNameFromJoinPoint(joinPoint));
		parallelJoinHelper.setTask(joinerTask);
		return null;
	}
	
	/**
	 * Get the objects array from list so as to call the required join annotated method.
	 * @param list List containing objects.
	 * @return Object[] containing all objects present in list
	 */
	protected Object[] getObjectArrayFromList(List<Object> list) {
		Object[] output = new Object[list.size()];
		for (int i=0; i<list.size(); i++) {
			output[i] = list.get(i);
		}
		return output;
	}

	/**
	 * This class object will act as notification that some joiner called till this place, also it contains the data.
	 * @author amit.khosla
	 *
	 */
	static class JoinResult {
		Object result;
	}
	/**
	 * From the parallel id, read all objects created by different controlled methods ran in parallel flow. 
	 * @param list List to be filled
	 * @param parallelTaskId Parallel task id
	 * @param taskId Task id
	 */
	protected void addAllResultObjectsTillNowInList(List<Object> list, int parallelTaskId, int taskId) {
		Map<Integer, Object> map = parallelJoinHelper.map.get(parallelTaskId);
		for (int i=0;i<taskId;i++) {
			Object val = map.get(i);
			if (val == null || val == ParallelJoinHelper.NULL_OBJECT) {
				continue;
			}
			if (val.getClass() == JoinResult.class) {
				//list.clear();
				list.add(((JoinResult) val).result);
				map.put(i,ParallelJoinHelper.NULL_OBJECT);
			} else {
				list.add(val);
				map.put(i,ParallelJoinHelper.NULL_OBJECT);
			}
		}
	}

	/**
	 * Handles controlled annotated methods.
	 * Controlled annotated methods are run in given executor. Current task waits for the execution to complete.
	 * This helps in throttling the execution, i.e., at a given time only specified number of executions are allowed for given process.
	 * In case it is running in Parallel flow, this just add it in task chain which will be handled in parallel.
	 * @param joinPoint Join point
	 * @param controlled Controlled
	 * @return Output of the annotated method
	 * @throws Throwable In case of exception in annotated mehtod
	 */
	@Around("execution(@org.ak.trafficController.annotations.api.Controlled * *(..)) && @annotation(parallel)")
	 public Object runControlled(ProceedingJoinPoint joinPoint, Controlled controlled) throws Throwable {
		TaskExecutorDetails taskExecutorDetail = taskHelper.getTaskExecutor(controlled, joinPoint);
		Task task = ParallelJoinHelper.getTask();
		boolean taskExecutorPresent = TaskExecutorsInUseThreadLocal.isTaskExecutorPresent(taskExecutorDetail.getName());
		if (taskExecutorPresent) {
			if (task == null) {
				logger.fine("already from same executor..so processing directly.");
				return joinPoint.proceed();
			}
		}
		String nameForTheTaskExecutor = getNameForTaskExecutor(controlled, taskExecutorDetail);
		TaskExecutor taskExecutor = taskExecutorDetail.getTaskExecutor();
		if (task !=null) {
			taskExecutor = addToTaskChainAsCalledFromParallel(joinPoint, controlled, task, taskExecutorPresent,
					nameForTheTaskExecutor, taskExecutor);
			return null;
		}
			
		return executeControlled(joinPoint, controlled, nameForTheTaskExecutor, taskExecutor);
	}

	/**
	 * Add to task chain when Controlled called from parallel flow.
	 * @param joinPoint Join point
	 * @param controlled Controlled
	 * @param task Parallel Task
	 * @param taskExecutorPresent Is task executor present
	 * @param nameForTheTaskExecutor Name of task executor
	 * @param taskExecutor Task executor
	 * @return Task executor
	 */
	protected TaskExecutor addToTaskChainAsCalledFromParallel(ProceedingJoinPoint joinPoint, Controlled controlled,
			Task task, boolean taskExecutorPresent, String nameForTheTaskExecutor, TaskExecutor taskExecutor) {
		int taskId = parallelJoinHelper.getObjectKeyForParalleldTask();
		int parallelTaskId = ParallelJoinHelper.getParallelId();
		logger.fine("already from same executor..so will be processed directly via different task.");
		
		RunnableToBeExecuted runnableToBeExecuted = ()->{
			ParallelJoinHelper.putObject(parallelTaskId, taskId, joinPoint.proceed());
		};
		/// TODO - IS this check required?
		if (taskExecutorPresent) {
			taskExecutor = task.getTaskExecutor();
		} else {
			runnableToBeExecuted = ()->{
				try {
					TaskExecutorsInUseThreadLocal.setTaskExecutor(nameForTheTaskExecutor);
					Object result = joinPoint.proceed();
					if (Objects.nonNull(result)) {
						ParallelJoinHelper.putObject(parallelTaskId, taskId, result);
					}
				} finally {
					TaskExecutorsInUseThreadLocal.removeTaskExecutor(nameForTheTaskExecutor);
				}
			};
		}
		String name = "ParallelId:" + parallelTaskId + " taskId:" +taskId + " " + getTaskNameFromJoinPoint(joinPoint);
		org.ak.trafficController.Task.TaskType taskType = convertAnnotationTaskTypeToFrameworkTaskType(controlled.taskType());
		((ParallelTask) task).addRunnables(taskType, taskExecutor,name, runnableToBeExecuted);
		return taskExecutor;
	}

	/**
	 * Get task name from join point
	 * @param joinPoint Join [pomt
	 * @return
	 */
	private String getTaskNameFromJoinPoint(ProceedingJoinPoint joinPoint) {
		return joinPoint.toShortString();
	}

	/**
	 * Get name of task executor for controlled
	 * @param parallel Controlled
	 * @param taskExecutorDetail Task executor details
	 * @return Name of task executor
	 */
	protected String getNameForTaskExecutor(Controlled parallel, TaskExecutorDetails taskExecutorDetail) {
		StringBuilder nameOfTaskExecutorBuilder = new StringBuilder();
		nameOfTaskExecutorBuilder.append(taskExecutorDetail.getName());
		if (parallel.taskType() == TaskType.SLOW) {
			nameOfTaskExecutorBuilder.append(":::::SLOW");
		}
		String nameForTheTaskExecutor = nameOfTaskExecutorBuilder.toString();
		return nameForTheTaskExecutor;
	}

	/**
	 * Execute controlled in direct flow.
	 * @param joinPoint Join point
	 * @param parallel Controlled
	 * @param nameForTheTaskExecutor Name of task executor
	 * @param taskExecutor Task executor
	 * @return Output of annotated method
	 * @throws Throwable In case annotated method throws exception
	 */
	protected Object executeControlled(ProceedingJoinPoint joinPoint, Controlled parallel,
			String nameForTheTaskExecutor, TaskExecutor taskExecutor) throws Throwable {
		AtomicReference<Throwable> throwableRef = new AtomicReference<Throwable>(null);
		AtomicReference<Object> value = new AtomicReference<Object>(null);
		
		RunnableToBeExecuted taskToWorkOn = ()-> {
			try{
				TaskExecutorsInUseThreadLocal.setTaskExecutor(nameForTheTaskExecutor);
				Object k = joinPoint.proceed();
				value.set(k);
			} catch (Throwable e) {
				logger.log(Level.WARNING, "Exception occured while executing a parallel request.", e);
				throwableRef.set(e);
			} finally {
				TaskExecutorsInUseThreadLocal.removeTaskExecutor(nameForTheTaskExecutor);
			}
		};
		
		
		switch (parallel.taskType()) {
		case NORMAL:
			taskExecutor.of(taskToWorkOn).start(parallel.waitTimeInMilliSeconds());
			break;
		case SLOW:
			taskExecutor.slowOf(taskToWorkOn).start(parallel.waitTimeInMilliSeconds());
		}
		Throwable throwable = throwableRef.get();
		if (throwable != null) {
			throw throwable;
		}
		return value.get();
	}
	
}
