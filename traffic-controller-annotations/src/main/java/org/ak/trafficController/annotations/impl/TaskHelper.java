package org.ak.trafficController.annotations.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;

import org.ak.trafficController.TaskExecutor;
import org.ak.trafficController.annotations.api.Constants;
import org.ak.trafficController.annotations.api.Controlled;
import org.ak.trafficController.annotations.api.Submit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;

@Named
public class TaskHelper {

	static final String VALID_NUMBERS_CHARS = "0123456789";
	static final String VALID_SPECIAL_CHARS_BETWEEN_NUMBERS = "_";
	
	ConcurrentHashMap<String, TaskExecutor> taskExecutors = new ConcurrentHashMap<>();
	
	public TaskExecutor getTaskExecutor(Submit async, ProceedingJoinPoint joinPoint) {
		String taskExecutorName = getTaskExecutorName(async);
		TaskExecutor taskExecutor = getExecutorByName(taskExecutorName);
		if (StringUtils.isEmpty(taskExecutorName)) {
			taskExecutorName = getNameFromJoinPointMethodSignature(joinPoint);
		}
		if (Objects.isNull(taskExecutor)) {
			if (taskExecutorName.equals(Constants.DEFAULT)) {
				taskExecutor = TaskExecutor.getInstance();
			}
			else {
				taskExecutor = getTaskExecutorBasedOnAsyncProps(async, joinPoint);
			}
			taskExecutors.put(taskExecutorName, taskExecutor);
		}
		return taskExecutor;
	}

	protected TaskExecutor getTaskExecutorBasedOnAsyncProps(Submit async, ProceedingJoinPoint joinPoint) {
		int maxConsumers = getConsumers(async.maxConsumer(), joinPoint);
		int maxSlowConsumers = getConsumers(async.maxSlowConsumer(), joinPoint);
		return getTaskExecutorForConsumersDetails(maxConsumers, maxSlowConsumers);
	}
	
	protected TaskExecutor getTaskExecutorBasedOnAsyncProps(Controlled async, ProceedingJoinPoint joinPoint) {
		int maxConsumers = getConsumers(async.maxConsumer(), joinPoint);
		int maxSlowConsumers = getConsumers(async.maxSlowConsumer(), joinPoint);
		return getTaskExecutorForConsumersDetails(maxConsumers, maxSlowConsumers);
	}

	protected TaskExecutor getTaskExecutorForConsumersDetails(int maxConsumers, int maxSlowConsumers) {
		TaskExecutor taskExecutor;
		if (maxConsumers > 0 || maxSlowConsumers > 0) {
			taskExecutor = TaskExecutor.getTaskExecutorWithDefinedNumberOfConsumers(maxConsumers, maxSlowConsumers);
		} else {
			taskExecutor = TaskExecutor.getInstance();
		}
		return taskExecutor;
	}

	protected String getNameFromJoinPointMethodSignature(ProceedingJoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		String className = joinPoint.getTarget().getClass().getName();
		String[] params = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
		StringBuilder name = new StringBuilder();
		name.append(className).append(".").append(methodName).append("[");
		for (String param : params) {
			name.append(param).append(",");
		}
		name.append("]");
		return name.toString();
	}

	protected int getConsumers(String numericValueOrMethodReturning, ProceedingJoinPoint joinPoint) {
		// TODO Auto-generated method stub
		int output = getNumberFromString(numericValueOrMethodReturning);
		if (output == -1) {//not a numeric string, thus a string
			Method[] methods = joinPoint.getTarget().getClass().getMethods();
			for (Method m : methods) {
				if (m.getName().equals(numericValueOrMethodReturning) && m.getParameters().length == 0) {
					try {
						output = (int) m.invoke(joinPoint.getTarget());
						break;
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return output;
	}

	protected String getTaskExecutorName(Submit submit) {
		// TODO Auto-generated method stub
		//if (async.executorName())
		return submit.executorName();
	}

	public TaskExecutorDetails getTaskExecutor(Controlled parallel, ProceedingJoinPoint joinPoint) {
		String taskExecutorName = parallel.executorName();
		TaskExecutor taskExecutor = getExecutorByName(taskExecutorName);
		if (StringUtils.isEmpty(taskExecutorName)) {
			taskExecutorName = getNameFromJoinPointMethodSignature(joinPoint);
		}
		if (Objects.isNull(taskExecutor)) {
			if (taskExecutorName.equals(Constants.DEFAULT)) {
				taskExecutor = TaskExecutor.getInstance();
			}
			else {
				taskExecutor = getTaskExecutorBasedOnAsyncProps(parallel, joinPoint);
			}
			taskExecutors.put(taskExecutorName, taskExecutor);
		}
		TaskExecutorDetails details = new TaskExecutorDetails().setName(taskExecutorName).setTaskExecutor(taskExecutor);
		return details;
		//return null;//TaskExecutor.getInstance();
	}

	protected TaskExecutor getExecutorByName(String string) {
		TaskExecutor taskExecutor = taskExecutors.get(string);
		if (!Objects.isNull(taskExecutor)) {
			return taskExecutor;
		}
		String[] commaSepratedStrings = string.split(",");
		int cores = Runtime.getRuntime().availableProcessors();
		if (commaSepratedStrings.length == 2) {
			//either by number or percentage expected.
			Integer[] integers = new Integer[2];
			for (int i=0;i<2;i++) {
				String numberOrPercentage = commaSepratedStrings[i];
				String numOrPer = numberOrPercentage.trim();
				if (numOrPer.endsWith("%")) {
					int percentFromString = getNumberFromString(numOrPer.substring(0, numOrPer.length()-1));
					if (percentFromString != 0) { 
						int value = percentFromString * cores / 100;
						if (value == 0) {
							value = 1;
						}
						integers[i] = value;
					} else {
						integers[i] = 0;
					}
				} else {
					integers[i] = getNumberFromString(numOrPer);
				}
			}
			if (integers[0] > -1 && integers[1] > -1) {
				TaskExecutor taskExecutorWithDefinedNumberOfConsumers = TaskExecutor.getTaskExecutorWithDefinedNumberOfConsumers(integers[0], integers[1]);
				taskExecutors.put(string, taskExecutorWithDefinedNumberOfConsumers);
				return taskExecutorWithDefinedNumberOfConsumers;
			}
		}
		
		return null;
	}

	
	protected int getNumberFromString(String number) {
		StringBuilder builder = new StringBuilder();
		char[] characters = number.toCharArray();
		for (int i=0;i<number.length();i++) {
			char character = characters[i];
			if (VALID_NUMBERS_CHARS.indexOf(character) > -1) {
				builder.append(character);
			} else if (VALID_SPECIAL_CHARS_BETWEEN_NUMBERS.indexOf(character) < 0) {
				return -1;
			}
		}
		return Integer.parseInt(builder.toString());
	}
	

	public void shutdown() {
		for (TaskExecutor taskExecutor : taskExecutors.values()) {
			taskExecutor.shutdown();
		}
	}
}
