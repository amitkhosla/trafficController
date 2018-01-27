package org.ak.trafficController.annotations.impl;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.TaskExecutor;
import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Submit;
import org.ak.trafficController.annotations.api.TaskType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;


@Aspect
@Named
public class AnnotationSupportImpl {

	Logger logger = Logger.getLogger(AnnotationSupportImpl.class.getName());
	
	@Inject
	TaskHelper taskHelper;
	
	@Around("execution(@org.ak.trafficController.annotations.api.Submit * *(..)) && @annotation(async)")
	 public Object runAsync(ProceedingJoinPoint joinPoint, Submit async) throws Throwable {
		
		Runnable taskToWorkOn = ()-> {
			try{
				joinPoint.proceed();	
			} catch (Throwable e) {
				logger.log(java.util.logging.Level.WARNING, "exception occured while running a submit request", e);
			}
		};
		TaskExecutor taskExecutor = taskHelper.getTaskExecutor(async, joinPoint);
		switch (async.taskType()) {
		case NORMAL: 
			taskExecutor.of(taskToWorkOn).submit();
			break;
		case SLOW :
			taskExecutor.slowOf(taskToWorkOn).submit();
		}
		return null;
	}
	
	@Around("execution(@org.ak.trafficController.annotations.api.Controlled * *(..)) && @annotation(parallel)")
	 public Object runAsync(ProceedingJoinPoint joinPoint, Controlled parallel) throws Throwable {
		AtomicReference<Throwable> throwableRef = new AtomicReference<Throwable>(null);
		AtomicReference<Object> value = new AtomicReference<Object>(null);
		TaskExecutorDetails taskExecutorDetail = taskHelper.getTaskExecutor(parallel, joinPoint);
		if (TaskExecutorsInUseThreadLocal.isTaskExecutorPresent(taskExecutorDetail.getName())) {
			logger.fine("already from same executor..so processing directly.");
			return joinPoint.proceed();
		}
		StringBuilder nameOfTaskExecutorBuilder = new StringBuilder();
		nameOfTaskExecutorBuilder.append(taskExecutorDetail.getName());
		if (parallel.taskType() == TaskType.SLOW) {
			nameOfTaskExecutorBuilder.append(":::::SLOW");
		}
		String nameForTheTaskExecutor = nameOfTaskExecutorBuilder.toString();
		Runnable taskToWorkOn = ()-> {
			try{
				TaskExecutorsInUseThreadLocal.setTaskExecutor(nameForTheTaskExecutor);
				Object k = joinPoint.proceed();
				value.set(k);
			} catch (Throwable e) {
				logger.log(Level.WARNING, "Exception occured while executing a parallel request.", e);
				throwableRef.set(e);
			} finally {
				TaskExecutorsInUseThreadLocal.removeTaskExcutor(nameForTheTaskExecutor);
			}
		};
		switch (parallel.taskType()) {
		case NORMAL:
			taskExecutorDetail.getTaskExecutor().of(taskToWorkOn).start();
			break;
		case SLOW:
			taskExecutorDetail.getTaskExecutor().slowOf(taskToWorkOn).start();
		}
		Throwable throwable = throwableRef.get();
		if (throwable != null) {
			throw throwable;
		}
		return value.get();
	}
	
}
