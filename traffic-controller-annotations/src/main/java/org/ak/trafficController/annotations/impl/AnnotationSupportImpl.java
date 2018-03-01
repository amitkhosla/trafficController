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


@Aspect
@Named
public class AnnotationSupportImpl {

	Logger logger = Logger.getLogger(AnnotationSupportImpl.class.getName());
	
	AtomicInteger parallelId = new AtomicInteger(0);
	
	@Inject
	TaskHelper taskHelper;
	
	@Inject
	ParallelJoinHelper parallelJoinHelper;
	
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
			AtomicReference<Object> output = new AtomicReference<Object>(joinPoint.proceed());
			Task cleanUpTask = parallelJoinHelper.getTask().then(()->{
				output.set(performCleanup(currentParallelId));
			});
			cleanUpTask.setName("Clean up task " + currentParallelId);
			parallelJoinHelper.setTask(cleanUpTask);
			//if (((MethodSignature) joinPoint.getSignature()).
			ParallelJoinHelper.taskChain.get().start(100000000);
			parallelJoinHelper.removeTask();
			Object val = output.get();
			if (val != null && val.getClass() == JoinResult.class) {
				val = ((JoinResult) val).result;
			}
			return val;
		} else {
			joinPoint.proceed();
			if (originalTask instanceof ParallelTask) {
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
			return null;
		}
	}
	
	protected Object performCleanup(int currentParallelId) {
		Map<Integer, Object> map = parallelJoinHelper.map.get(currentParallelId);
		Object obj = map.get((map.size()));
		parallelJoinHelper.map.remove(parallelId);
		return obj;
	}

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

	protected org.ak.trafficController.Task.TaskType convertAnnotationTaskTypeToFrameworkTaskType(TaskType taskType) {
		return taskType == TaskType.NORMAL ? org.ak.trafficController.Task.TaskType.NORMAL : org.ak.trafficController.Task.TaskType.SLOW;
	}

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
	
	protected Object[] getObjectArrayFromList(List<Object> list) {
		Object[] output = new Object[list.size()];
		for (int i=0; i<list.size(); i++) {
			output[i] = list.get(i);
		}
		return output;
	}

	static class JoinResult {
		Object result;
	}
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

	@Around("execution(@org.ak.trafficController.annotations.api.Controlled * *(..)) && @annotation(parallel)")
	 public Object runControlled(ProceedingJoinPoint joinPoint, Controlled parallel) throws Throwable {
		TaskExecutorDetails taskExecutorDetail = taskHelper.getTaskExecutor(parallel, joinPoint);
		Task task = ParallelJoinHelper.getTask();
		boolean taskExecutorPresent = TaskExecutorsInUseThreadLocal.isTaskExecutorPresent(taskExecutorDetail.getName());
		if (taskExecutorPresent) {
			if (task == null) {
				logger.fine("already from same executor..so processing directly.");
				return joinPoint.proceed();
			}
		}
		String nameForTheTaskExecutor = getNameForTaskExecutor(parallel, taskExecutorDetail);
		TaskExecutor taskExecutor = taskExecutorDetail.getTaskExecutor();
		if (task !=null) {
			taskExecutor = addToTaskChainAsCalledFromParallel(joinPoint, parallel, task, taskExecutorPresent,
					nameForTheTaskExecutor, taskExecutor);
			return null;
		}
			
		return executeControlled(joinPoint, parallel, nameForTheTaskExecutor, taskExecutor);
	}

	/**
	 * @param joinPoint
	 * @param parallel
	 * @param task
	 * @param taskExecutorPresent
	 * @param nameForTheTaskExecutor
	 * @param taskExecutor
	 * @return
	 */
	protected TaskExecutor addToTaskChainAsCalledFromParallel(ProceedingJoinPoint joinPoint, Controlled parallel,
			Task task, boolean taskExecutorPresent, String nameForTheTaskExecutor, TaskExecutor taskExecutor) {
		int taskId = parallelJoinHelper.getObjectKeyForParalleldTask();
		int parallelTaskId = ParallelJoinHelper.getParallelId();
		logger.fine("already from same executor..so will be processed directly via different task.");
		
		RunnableToBeExecuted runnableToBeExecuted = ()->{
			ParallelJoinHelper.putObject(parallelTaskId, taskId, joinPoint.proceed());
		};
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
		org.ak.trafficController.Task.TaskType taskType = convertAnnotationTaskTypeToFrameworkTaskType(parallel.taskType());
		((ParallelTask) task).addRunnables(taskType, taskExecutor,name, runnableToBeExecuted);
		return taskExecutor;
	}

	private String getTaskNameFromJoinPoint(ProceedingJoinPoint joinPoint) {
		// TODO Auto-generated method stub
		return joinPoint.toShortString();
	}

	/**
	 * @param parallel
	 * @param taskExecutorDetail
	 * @return
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
	 * @param joinPoint
	 * @param parallel
	 * @param nameForTheTaskExecutor
	 * @param taskExecutor
	 * @return
	 * @throws Throwable
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
