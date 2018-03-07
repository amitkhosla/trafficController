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

/**
 * This class is helper for {@link AnnotationSupportImpl} class.
 * This helps in finding and managing task executors.
 * @author amit.khosla
 *
 */
@Named
public class TaskHelper {

	static final String VALID_NUMBERS_CHARS = "0123456789";
	static final String VALID_SPECIAL_CHARS_BETWEEN_NUMBERS = "_";
	
	/**
	 * Task executors map by name.
	 */
	ConcurrentHashMap<String, TaskExecutor> taskExecutors = new ConcurrentHashMap<>();
	
	/**
	 * Get task executor for given Submit and join point.
	 * If name passed in Submit, will be used else will be derived from join point.
	 * @param async Submit
	 * @param joinPoint Join point
	 * @return Task executor
	 */
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

	/**
	 * Get task executor for submit.
	 * This method creates task executor with max consumer details.
	 * @param async Submit
	 * @param joinPoint Join point
	 * @return Task executor for required inputs
	 */
	protected TaskExecutor getTaskExecutorBasedOnAsyncProps(Submit async, ProceedingJoinPoint joinPoint) {
		int maxConsumers = getConsumers(async.maxConsumer(), joinPoint);
		int maxSlowConsumers = getConsumers(async.maxSlowConsumer(), joinPoint);
		return getTaskExecutorForConsumersDetails(maxConsumers, maxSlowConsumers);
	}
	
	/**
	 * Get task executor for Controlled.
	 * This method creates task executor with max consumer details.
	 * @param async Controlled
	 * @param joinPoint Join point
	 * @return Task executor for required inputs
	 */
	protected TaskExecutor getTaskExecutorBasedOnAsyncProps(Controlled async, ProceedingJoinPoint joinPoint) {
		int maxConsumers = getConsumers(async.maxConsumer(), joinPoint);
		int maxSlowConsumers = getConsumers(async.maxSlowConsumer(), joinPoint);
		return getTaskExecutorForConsumersDetails(maxConsumers, maxSlowConsumers);
	}

	/**
	 * Task executor for required max consumers and max slow consumers.
	 * @param maxConsumers Max consumer
	 * @param maxSlowConsumers Max slow consumers
	 * @return Task executor for required inputs
	 */
	protected TaskExecutor getTaskExecutorForConsumersDetails(int maxConsumers, int maxSlowConsumers) {
		TaskExecutor taskExecutor;
		if (maxConsumers > 0 || maxSlowConsumers > 0) {
			taskExecutor = TaskExecutor.getTaskExecutorWithDefinedNumberOfConsumers(maxConsumers, maxSlowConsumers);
		} else {
			taskExecutor = TaskExecutor.getInstance();
		}
		return taskExecutor;
	}

	/**
	 * Get name from join point using method name and class name.
	 * @param joinPoint Join point
	 * @return Name to be used of Task executor
	 */
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

	/**
	 * Get consumers count.
	 * @param numericValueOrMethodReturning Value to be found from caller
	 * @param joinPoint Join point
	 * @return Number of consumers
	 */
	protected int getConsumers(String numericValueOrMethodReturning, ProceedingJoinPoint joinPoint) {
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

	/**
	 * Name provided in Submit.
	 * @param submit Submit
	 * @return Name of executor
	 */
	protected String getTaskExecutorName(Submit submit) {
		return submit.executorName();
	}

	/**
	 * Get task executor for controlled.
	 * @param parallel Controlled
	 * @param joinPoint Join point
	 * @return TaskExecutor details
	 */
	public TaskExecutorDetails getTaskExecutor(Controlled parallel, ProceedingJoinPoint joinPoint) {
		String taskExecutorName = parallel.executorName();
		if (parallel.maxConsumer().equals("0") && parallel.maxSlowConsumer().equals("0") && taskExecutorName.isEmpty()) {
			taskExecutorName = Constants.DEFAULT;
		}
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

	static int cores = Runtime.getRuntime().availableProcessors();
	/**
	 * Get Task executor by name. Name can have numbers and percentages.
	 * @param string Name of task executor
	 * @return Task executor
	 */
	protected TaskExecutor getExecutorByName(String string) {
		TaskExecutor taskExecutor = taskExecutors.get(string);
		if (!Objects.isNull(taskExecutor)) {
			return taskExecutor;
		}
		String[] commaSepratedStrings = string.split(",");
		
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

	
	/**
	 * Get number from String.
	 * Returns -1 if not numeric or not in percentage
	 * Returns value if not in percentage
	 * Returns value calculated as percentage given of number of cores
	 * @param number String from which number is to be returned
	 * @return -1 for non numeric and non percentage, otherwise value
	 */
	protected int getNumberFromString(String number) {
		boolean percent = false;
		if (number.endsWith("%")) {
			number = number.substring(0,number.length() - 1);
			percent = true;
		}
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
		int numericValue = Integer.parseInt(builder.toString());
		if (percent) {
			numericValue = getPercentageValue(numericValue);
		}
		return numericValue;
	}

	/**
	 * Get number for given percentage.
	 * @param numericValue Value as percentage
	 * @return Value
	 */
	private int getPercentageValue(int numericValue) {
		if (numericValue > 0) {
			numericValue = numericValue * cores / 100;
			if (numericValue == 0 ) {
				numericValue = 1;
			}
		}
		return numericValue;
	}
	

	/**
	 * Shut down all the task executors created by this class.
	 */
	public void shutdown() {
		for (TaskExecutor taskExecutor : taskExecutors.values()) {
			taskExecutor.shutdown();
		}
	}
}
