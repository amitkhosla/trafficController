package org.ak.trafficController.annotations.impl;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;

import org.ak.trafficController.annotations.api.Submit;
import org.ak.trafficController.TaskExecutor;
import org.ak.trafficController.annotations.api.Controlled;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
@Named
public class AnnotationSupportImpl {

	@Inject
	TaskHelper taskHelper;
	
	@Around("execution(@org.ak.trafficController.annotations.api.Submit * *(..)) && @annotation(async)")
	 public Object runAsync(ProceedingJoinPoint joinPoint, Submit async) throws Throwable {
		taskHelper.getTaskExecutor(async, joinPoint).of(()-> {
			try{
				joinPoint.proceed();	
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}).submit();
		return null;
	}
	
	@Around("execution(@org.ak.trafficController.annotations.api.Controlled * *(..)) && @annotation(parallel)")
	 public Object runAsync(ProceedingJoinPoint joinPoint, Controlled parallel) throws Throwable {
		AtomicReference<Throwable> throwableRef = new AtomicReference<Throwable>(null);
		AtomicReference<Object> value = new AtomicReference<Object>(null);
		TaskExecutorDetails taskExecutorDetail = taskHelper.getTaskExecutor(parallel, joinPoint);
		if (TaskExecutorsInUseThreadLocal.isTaskExecutorPresent(taskExecutorDetail.getName())) {
			System.out.println("already from same executor..so processing directly.");
			return joinPoint.proceed();
		}
		taskExecutorDetail.getTaskExecutor().of(()-> {
			try{
				TaskExecutorsInUseThreadLocal.setTaskExecutor(taskExecutorDetail.getName());
				Object k = joinPoint.proceed();
				value.set(k);
			} catch (Throwable e) {
				e.printStackTrace();
				throwableRef.set(e);
			} finally {
				TaskExecutorsInUseThreadLocal.removeTaskExcutor(taskExecutorDetail.getName());
			}
		}).start();
		Throwable throwable = throwableRef.get();
		if (throwable != null) {
			throw throwable;
		}
		return value.get();
	}
	
}
