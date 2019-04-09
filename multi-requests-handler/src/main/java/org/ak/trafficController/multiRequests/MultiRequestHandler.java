/**
 * 
 */
package org.ak.trafficController.multiRequests;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

/**
 * This class handles multiple concurrent requests for same operation.
 * @author amit.khosla
 *
 */
public class MultiRequestHandler {
	static Gson gson = new Gson();
	static Logger logger = Logger.getLogger(MultiRequestHandler.class.getName());

	TimeSeriesList list = new TimeSeriesList("MultiRequestHandler");
	
	static MultiRequestHandler hanlderInstance = new MultiRequestHandler(); 
	
	public MultiRequestHandler getInstance() {
		return hanlderInstance;
	}
	
	Map<String, MultiRequestDTO> ran = new ConcurrentHashMap<>();
	Map<String, LocalDateTime> running = new ConcurrentHashMap<>();
	
	AtomicInteger returnedWithoutExecution = new AtomicInteger(0);
	AtomicInteger returnedWithoutWaitingToExecutedAsAsync = new AtomicInteger(0);
	
	public MultiRequestHandler() {
		this.list.setConsumer(m->ran.remove(m.getName()));
	}
	
	/**
	 * For the cases where we do not want to keep anything in cache and we are looking for only one process should run at a given time.
	 * @param name Unique name
	 * @param supplier Supplier which is working on this
	 * @param <T> Type of supplier, output type
	 * @return Output of the supplier
	 */
	public <T> T process(String name, Supplier<T> supplier) {
		return process(supplier, false, name, 1000l, 0,0);
	}
	
	/**
	 * Process List of items out of which some or all or none might be either running or just completed.
	 * Hence will save extra calls for the records which are already running or ran.
	 * For the items which are not run will be passed to the input function to find the result. 
	 * @param consumer Consumer which will be consuming list which is not already run or currently running
	 * @param collection Collection having the records
	 * @param uniqueName Unique name of the process, actual name will be derived from the item in collection as well
	 * @param expiry Expiry for which we want to not run the process again
	 * @param shouldNotWait If set will not wait for the items which are already running
	 * @param <T> Type of the item which we want to process for
	 */
	public <T> void processList(Consumer<List<T>> consumer, List<T> collection, String uniqueName, long expiry, boolean shouldNotWait) {
		Function<List<T>,Map<T,Object>> function = list->{
			consumer.accept(list);
			return new HashMap<>();
		};
		processListToMap(function, collection, uniqueName, expiry, shouldNotWait);
	}
	
	/**
	 * Process List of items out of which some or all or none might be either running or just completed.
	 * Hence will save extra calls for the records which are already running or ran.
	 * For the items which are not run will be passed to the input function to find the result. 
	 * @param function Function which will be applied on list which is not already run or currently running
	 * @param collection Collection having the records
	 * @param uniqueName Unique name of the process, actual name will be derived from the item in collection as well
	 * @param expiry Expiry for which we want to not run the process again
	 * @param shouldNotWait If set will return the null value for items which are already running
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 * @return Map of result from already run, currently running and post running function for items left
	 */
	public <T,R> Map<T,R> processListToMap(Function<List<T>, Map<T,R>> function, List<T> collection, String uniqueName, long expiry, boolean shouldNotWait) {
		if (!shouldNotWait) {
			return processListToMap(function, collection, uniqueName, expiry);
		}
		Map<T,R> result = new HashMap<>();
		Map<String,T> toBeRun = new HashMap<>();
		Map<String,T> waiting = new HashMap<>();
		populateRanWaitingAndShouldRun(collection, uniqueName, result, toBeRun, waiting);
		for (T waitingRes : waiting.values()) {
			result.put(waitingRes, null);
		}
		runMissings(function, expiry, result, toBeRun);
		return result;
	}
	
	/**
	 * Process List of items out of which some or all or none might be either running or just completed.
	 * Hence will save extra calls for the records which are already running or ran.
	 * For the items which are not run will be passed to the input function to find the result. 
	 * @param function Function which will be applied on each record in list which is not already run or currently running
	 * @param collection Collection having the records
	 * @param uniqueName Unique name of the process, actual name will be derived from the item in collection as well
	 * @param expiry Expiry for which we want to not run the process again
	 * @param shouldNotWait If set will return the null value for items which are already running
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 * @return Map of result from already run, currently running and post running function for items left
	 */
	public <T,R> Map<T,R> processListByConvertingIndividualRecords(Function<T, R> function, List<T> collection, String uniqueName, long expiry, boolean shouldNotWait) {
		Function<List<T>, Map<T,R>> listFunction = list->{
			Map<T,R> map = new HashMap();
			list.forEach(item->{
				map.put(item, function.apply(item));
			});
			return map;
		};
		return processListToMap(listFunction, collection, uniqueName, expiry, shouldNotWait);
	}
	
	
	/**
	 * Process List of items out of which some or all or none might be either running or just completed.
	 * Hence will save extra calls for the records which are already running or ran.
	 * For the items which are not run will be passed to the input function to find the result. 
	 * @param function Function which will be applied on each record in list which is not already run or currently running
	 * @param collection Collection having the records
	 * @param uniqueName Unique name of the process, actual name will be derived from the item in collection as well
	 * @param expiry Expiry for which we want to not run the process again
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 * @return Map of result from already run, currently running and post running function for items left
	 */
	public <T,R> Map<T,R> processListByConvertingIndividualRecords(Function<T, R> function, List<T> collection, String uniqueName, long expiry) {
		Function<List<T>, Map<T,R>> listFunction = list->{
			Map<T,R> map = new HashMap();
			list.forEach(item->{
				map.put(item, function.apply(item));
			});
			return map;
		};
		return processListToMap(listFunction, collection, uniqueName, expiry, false);
	}
	
	/**
	 * Process List of items out of which some or all or none might be either running or just completed.
	 * Hence will save extra calls for the records which are already running or ran.
	 * For the items which are not run will be passed to the input function to find the result. 
	 * @param function Function which will be applied on each record in list which is not already run or currently running
	 * @param collection Collection having the records
	 * @param uniqueName Unique name of the process, actual name will be derived from the item in collection as well
	 * @param expiry Expiry for which we want to not run the process again
	 * @param <T> Type of elements in list
	 * @param shouldNotWait Should not wait for processing if already done
	 */
	public <T> void processListByConvertingIndividualRecords(Consumer<T> function, List<T> collection, String uniqueName, long expiry, boolean shouldNotWait) {
		Consumer<List<T>> listFunction = list->{
			list.forEach(item->{
				function.accept(item);
			});
		};
		processList(listFunction, collection, uniqueName, expiry, shouldNotWait);
	}
	
	
	/**
	 * Process List of items out of which some or all or none might be either running or just completed.
	 * Hence will save extra calls for the records which are already running or ran.
	 * For the items which are not run will be passed to the input function to find the result. 
	 * @param function Function which will be applied on each record in list which is not already run or currently running
	 * @param collection Collection having the records
	 * @param uniqueName Unique name of the process, actual name will be derived from the item in collection as well
	 * @param expiry Expiry for which we want to not run the process again
	 * @param <T> Type of elements in list
	 */
	public <T> void processListByConvertingIndividualRecords(Consumer<T> function, List<T> collection, String uniqueName, long expiry) {
		processListByConvertingIndividualRecords(function, collection, uniqueName, expiry, false);
	}
	
	/**
	 * Process List of items out of which some or all or none might be either running or just completed.
	 * Hence will save extra calls for the records which are already running or ran.
	 * For the items which are not run will be passed to the input function to find the result. 
	 * @param function Function which will be applied on list which is not already run or currently running
	 * @param collection Collection having the records
	 * @param uniqueName Unique name of the process, actual name will be derived from the item in collection as well
	 * @param expiry Expiry for which we want to not run the process again
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 * @return Map of result from already run, currently running and post running function for items left
	 */
	public <T,R> Map<T,R> processListToMap(Function<List<T>, Map<T,R>> function, List<T> collection, String uniqueName, long expiry) {
		Map<T,R> result = new HashMap<>();
		Map<String,T> toBeRun = new HashMap<>();
		Map<String,T> waiting = new HashMap<>();
		Map<T, R> resultFromWait = new HashMap<>();
		Map<String, T> postWaitCouldNotGet = new HashMap<>();
		populateRanWaitingAndShouldRun(collection, uniqueName, result, toBeRun, waiting);
		if (waiting.isEmpty()) {
			runMissings(function, expiry, result, toBeRun);
		} else {
			Thread waitingThread = startLookingForWaitingThread(waiting, resultFromWait, postWaitCouldNotGet);
			runMissings(function, expiry, result, toBeRun);
			waitForWaitings(result, resultFromWait, waitingThread);
			runMissings(function, expiry, result, postWaitCouldNotGet);
		}
		return result;
	}
	
	/**
	 * This method is part of the list iteration.
	 * This part is waiting for delta which is waiting for other processes to complete.
	 * Once that is done, we proceed forward.
	 * Waiting thread is filling resultFromWait.
	 * @param result Result which we want to return to the consumer
	 * @param resultFromWait The waiting result which will be filled once the waiting part completes
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 * @param waitingThread Thread which is running waiting part
	 */
	protected <T, R> void waitForWaitings(Map<T, R> result, Map<T, R> resultFromWait, Thread waitingThread) {
		try {
			waitingThread.join();
		} catch (InterruptedException e) {
			logger.log(Level.FINE, "Interruption received when we were looking to join the thread which is waiting and populating the data.", e);
		}
		result.putAll(resultFromWait);
	}
	/**
	 * Run for missing items if any.
	 * If there is no record present in toBeRun, nothing will be run. The use case is where we already have everything got from running and waiting parts.
	 * @param function Function which is processing the list to output
	 * @param expiry Expiry which will be used to keep the result of individual records
	 * @param result Result map which will be populated via running the function
	 * @param toBeRun The items which is needed to be run
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 */
	protected <T, R> void runMissings(Function<List<T>, Map<T, R>> function, long expiry, Map<T, R> result,
			Map<String, T> toBeRun) {
		if (!toBeRun.isEmpty()) {
			result.putAll(populateByrunning(toBeRun, function, expiry));
		}
	}
	
	/**
	 * This creates the thread which looks for items we need to wait for.
	 * @param waiting Waiting map which contains name to object mappings
	 * @param resultFromWait Post processing, result will be pushed to this
	 * @param postWaitCouldNotGet A few racing condition where we found the records but it expired before we could use 
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 * @return The thread which is running waiting logic
	 */
	protected <T, R> Thread startLookingForWaitingThread(Map<String, T> waiting, Map<T, R> resultFromWait,
			Map<String, T> postWaitCouldNotGet) {
		///TODO - MayBe we might need to think about the creation of new thread should be avoided.
		Thread t1 = new Thread(()->populateFromWaiting(waiting, resultFromWait, postWaitCouldNotGet));
		t1.start();
		return t1;
	}
	
	/**
	 * This method filters the collection from the items which are already run, or are running and the remaining for which we have to run.
	 * @param collection The collection of records for which we have to find results
	 * @param uniqueName The uniqueName for the record
	 * @param result Result which is populated from already run data
	 * @param toBeRun Items which needs to be run is populated in the method
	 * @param waiting waiting items for which other processes are running
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 */
	protected <R, T> void populateRanWaitingAndShouldRun(List<T> collection, String uniqueName, Map<T, R> result,
			Map<String, T> toBeRun, Map<String, T> waiting) {
		for (T item : collection) {
			String name = getName(uniqueName, item);
			Object obj = getIfAlreadyRan(name);
			if (obj != null) { //already run.
				addOutputFromAlreadyRunResults(result, item, obj);
			} else if (running.get(name) != null) {
					waiting.put(name, item);
			} else {
				name = name.intern();
				synchronized (name) {
					Object obj1 = getIfAlreadyRan(name);
					if (obj1 != null) { //already run.
						addOutputFromAlreadyRunResults(result, item, obj1);
					} else if (running.get(name) != null) {
						waiting.put(name, item);
					} else {
						running.put(name, LocalDateTime.now());
					}
					toBeRun.put(name, item);
				}
			}
		};
	}

	/**
	 * @param result
	 * @param item
	 * @param obj
	 */
	private <R, T> void addOutputFromAlreadyRunResults(Map<T, R> result, T item, Object obj) {
		if (obj == MultiRequestDTO.NULL_OBJECT) {
			obj = null;
		}
		result.put(item, (R) obj);
	}
	
	/**
	 * Populate by running tbe items.
	 * ToBeRun map contains the name to item mapping. Name is used to make it run once for given time.
	 * Function runs on the list of the items extracted from the map.
	 * @param toBeRun ToBeRun map contains the name to item mapping; Name is used to make it run once for given time
	 * @param function Function which should be run on items; for which we do not have results available
	 * @param expiry Expiry of each record to retain till which we do not want to rerun
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 * @return Output of the function which extracts outputs for the list
	 */
	protected <T,R> Map<T, R> populateByrunning(Map<String, T> toBeRun, Function<List<T>, Map<T,R>> function, long expiry) {
		List<T> list = new ArrayList<>(toBeRun.values());
		/*toBeRun.forEach((k,v)->{
			list.add(v);
			running.put(k, LocalDateTime.now());
		});*/
		try {
			Map<T, R> result = function.apply(list);
			toBeRun.forEach((k,v)-> {
				ran.put(k, new MultiRequestDTO()
						.setName(k)
						.setTime(expiry)
						.setOutput(result.get(v)));
			});
			return result;
		} finally {
			toBeRun.forEach((k,v)-> {
				running.remove(k);
			});
		}
	}
	
	/**
	 * Populate from other processes which are running for given items.
	 * Thus, wait for those process(es) to complete and keep populating the records.
	 * @param waiting Waiting map containing all records for which we have to wait; key being name for the item and item as value
	 * @param result Result to be populated from the waiting records
	 * @param returnedNull A few racing condition where we found the records but it expired before we could use 
	 * @param <T> Type of elements in list
	 * @param <R> Type of result of elements
	 */
	protected <T,R> void populateFromWaiting(Map<String, T> waiting, Map<T, R> result, Map<String, T> returnedNull) {
		while (!waiting.isEmpty()) {
			List<String> list = new ArrayList<>(waiting.keySet());
			list.forEach(s->{
				Object r = getIfAlreadyRan(s);
				if (r != null) {
					T t = waiting.remove(s);
					addOutputFromAlreadyRunResults(result, t, r);
				} else if (running.get(s) == null) {
					//lets try once more
					Object r1 = getIfAlreadyRan(s);
					if (r1 != null) {
						T t = waiting.remove(s);
						addOutputFromAlreadyRunResults(result, t, r1);
					} else {
						returnedNull.put(s, waiting.remove(s));
					}
				}
			});
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				logger.log(Level.FINE, "Could Not sleep", e);
			}
		}
		
	}
	
	
	/**
	 * Process the runnable while keeping control that it runs only once for a second. Async flag also let it continue event if already in progress cases.
	 * We will try to find the uniqueness from stack trace and use the caller of this method.
	 * @param runnable Runnable to be run
	 * @param isAsync Should we wait for process to complete if running by other thread
	 */
	public void process(Runnable runnable, boolean isAsync) {
		String name = getUniqueName();
		Supplier s = ()->{
			runnable.run();
			return null;
		};
		process(s, isAsync, name, 1000l, 0,0);
	}

	
	/**
	 * Process a function for a given input. The name is derived from the unique name along with the unique names.
	 * @param function Function which will be applied on the input
	 * @param input Data to be transformed in this process
	 * @param isAsync Should we skip if already in running via other thread; in such case will return null
	 * @param name Unique name
	 * @param timeToKeep Expiry time for which we do not want to run this again and will return the result processed once
	 * @param numberOfRetries Retries count in case of failure
	 * @param retryIntervals Waiting between reties
	 * @param <T> type of input
	 * @param <R> Return type of function which will be applied to the input
	 * @return Output of the function or null in case async is true and process already in use
	 */
	public <T, R> R process(Function<T,R> function, T input, boolean isAsync, String name, Long timeToKeep, int numberOfRetries, int retryIntervals) {
		Supplier<R> supplier = ()->function.apply(input);
		//name = name + input
		String passingName = getName(name, input);
		return process(supplier, isAsync, passingName, timeToKeep, numberOfRetries, retryIntervals);
	}
	
	/**
	 * The name which will be used to identify if a process already run or not.
	 * We look for unique name passed and also append all inputs. 
	 * @param name Unique name
	 * @param input Inputs to the method
	 * @param <T> Type of input items
	 * @return The name which will be used to identify the operation
	 */
	protected <T> String getName(String name, T... input) {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		sb.append(name).append((char)(1));
		if (input != null) {
			for (T i : input) {
				sb.append(getDetailsFromObject(i)).append((char)2);
			}
		}
		return sb.toString();
	}
	
	static Set<Class> directClasses = new HashSet<>();
	static {
		directClasses.add(Integer.class);
		directClasses.add(Double.class);
		directClasses.add(Long.class);
		directClasses.add(String.class);
		directClasses.add(Float.class);
		directClasses.add(Character.class);
		directClasses.add(Boolean.class);
		directClasses.add(Byte.class);
		directClasses.add(Date.class);
		directClasses.add(LocalDateTime.class);
		directClasses.add(LocalDate.class);
		directClasses.add(LocalTime.class);
	}

	/**
	 * This method is in use to create unique name for a request. The object used is transformed to name.
	 * @param obj Object for which we want name
	 * @return qualified name for the given object
	 */
	protected String getDetailsFromObject(Object obj) {
		String s;
		if (obj.getClass().isPrimitive() || directClasses.contains(obj.getClass())) {
			s=obj.toString();
		} else {
			s = getStringAsJson(obj);
		}
		return s;
	}
	
	
	/**
	 * Get json string for given object
	 * @param object Object
	 * @return json string
	 */
	protected String getStringAsJson(Object object)  {
		return gson.toJson(object);
	}
	
	/**
	 * Process a process which is done by passed supplier once for the given time.
	 * Name passed is unique name for which all processes are seen to be working fine.
	 * If tried again within expiration limits, will be returned with same result.
	 * @param supplier The actually process
	 * @param isAsync Should wait for any process already running, if set true will not wait and return null
	 * @param name Unique name for which we want process to run once
	 * @param timeToKeep Expiration limit
	 * @param numberOfRetries Number of retries to process in case of any failure
	 * @param retryIntervals Duration between consecutive retries
	 * @param <T> Type of output
	 * @return Output of supplier or if already run then previous result or null if already in running in case of isAsync set as true
	 */
	public <T> T process(Supplier<T> supplier, boolean isAsync, String name, Long timeToKeep, int numberOfRetries, int retryIntervals) {
		name = name.intern();
		Object obj = getIfAlreadyRan(name);
		if (obj != null) {
			return obj == MultiRequestDTO.NULL_OBJECT ? null : (T) obj;
		}
		boolean shouldRun = shouldRunFurther(name);
		
		if (shouldRun) {
			return processFlow(supplier, name, timeToKeep, numberOfRetries, retryIntervals);
		} else {
			//not required to run. 
			return waitingFlow(supplier, isAsync, name, timeToKeep);
					
		}
	}
	/**
	 * Validate if already ran within the expiration limit set at time of running.
	 * @param name Unique name against which process was run
	 * @param <T> Type of expected result
	 * @return Result if already ran else null
	 */
	protected Object getIfAlreadyRan(String name) {
		MultiRequestDTO multiRequestDTO = ran.get(name);
		if (multiRequestDTO != null) {
			Object obj = multiRequestDTO.getOutput();
			if (obj == MultiRequestDTO.INVALIDATED_OBJECT) {
				ran.remove(name);
			} else {
				return obj;
			}
		}
		return null;
	}
	
	/**
	 * Process a process which is done by passed supplier once for the given time.
	 * Name passed is unique name for which all processes are seen to be working fine.
	 * If tried again within expiration limits, will be returned with same result.
	 * @param supplier The actually process
	 * @param name Unique name for which we want process to run once
	 * @param timeToKeep Expiration limit
	 * @param numberOfRetries Number of retries to process in case of any failure
	 * @param retryIntervals Duration between consecutive retries
	 * @param <T> Type of output
	 * @return Output of supplier or if already run then previous result
	 */
	protected <T> T processFlow(Supplier<T> supplier, String name, Long timeToKeep, int numberOfRetries,
			int retryIntervals) {
		try {
			return getFromSupplierAndBuild(supplier, name, timeToKeep);
		} catch (RuntimeException e) {
			logger.log(Level.INFO, "retrying for " + name);
			for (int i=0; i< numberOfRetries; i++) {
				try {
					Thread.sleep(retryIntervals);
				} catch (InterruptedException e1) {
					logger.log(Level.FINE, "sleep interrupted, we are proceeding for next retry.");
				}
				try {
					return getFromSupplierAndBuild(supplier, name, timeToKeep);
				} catch (RuntimeException re) {
					logger.log(Level.INFO, re, ()->("Retrying again for " + name));
				}
			}
			ran.put(name, new MultiRequestDTO().setName(name).setException(e).setTimeWhenStarted(LocalDateTime.now()).setTime(timeToKeep));
			throw e;
		}
		finally {
			running.remove(name);
			synchronized (name) {
				name.notifyAll();
			}
		}
	}
	
	
	
	/**
	 * The method which executes in case process is configured to wait for results from other processes.
	 * If we cannot get result from other processes, we execute again.
	 * @param supplier Supplier which will be executed
	 * @param isAsync Should we wait or not, async means we do not need to wait for already running process
	 * @param name Unique name
	 * @param timeToKeep Expiration time in case of successful execution
	 * @param <T> Type of suppliers
	 * @return result from supplier or from other processes
	 */
	protected <T> T waitingFlow(Supplier<T> supplier, boolean isAsync, String name, Long timeToKeep) {
		MultiRequestDTO multiRequestDTO;
		if (isAsync) {
			returnedWithoutWaitingToExecutedAsAsync.incrementAndGet();
			return null;
		}
		if (running.get(name) != null) {
				while (running.get(name) != null) {
					synchronized (name) {
						try {
							name.wait(100);
						} catch (InterruptedException e) {
							logger.log(Level.FINE, "could not wait while processing for " + name, e);
						}
					}
				}
			} 
		multiRequestDTO = ran.get(name);
		if (multiRequestDTO != null) {
			Object o = multiRequestDTO.getOutput();
			if (o != MultiRequestDTO.INVALIDATED_OBJECT) {
				logger.log(Level.FINER,returnedWithoutExecution.incrementAndGet() + " returned without executing");
				return (T) o;
			}
		} //we have not recieved value from cache, thus re running. 
		returnedWithoutExecution.decrementAndGet();
		return getFromSupplierAndBuild(supplier, name, timeToKeep);
	}
	
	/**
	 * This method verified if already running. In case already running in other process it returns false else return true
	 * @param name Name for which the process is running
	 * @return In case already running in other process it returns false else return true
	 */
	private boolean shouldRunFurther(String name) {
		boolean shouldRun = false;
		synchronized(name) {
			if (!hasAlreadyRunOrRunning(name)) {
				running.put(name, LocalDateTime.now());
				shouldRun = true;
			}
		}
		return shouldRun;
	}

	/**
	 * Get from supplier and build the multi request dto and return the data.
	 * @param supplier Supplier to be run
	 * @param name Name against which we will persist it
	 * @param timeToKeep Expiry till when we do not want it to run again
	 * @param <T> type of supplier
	 * @return Output from the supplier
	 */
	private <T> T getFromSupplierAndBuild(Supplier<T> supplier, String name, Long timeToKeep) {
		T output = supplier.get();
		MultiRequestDTO mutliRequestDTO = new MultiRequestDTO().setName(name).setOutput(output).setTimeWhenStarted(LocalDateTime.now()).setTime(timeToKeep);
		ran.put(name, mutliRequestDTO);
		list.add(mutliRequestDTO);
		return output;
	}

	/**
	 * Has already run or currently running?
	 * @param name Unique name
	 * @return true if running or already run
	 */
	protected boolean hasAlreadyRunOrRunning(String name) {
		boolean shouldReturn = false;
		if (running.get(name) != null || ran.get(name) != null) {
			shouldReturn = true;
		}
		return shouldReturn;
	}

	/**
	 * Get unique name from the caller of the method.
	 * @return Name from the stack trace
	 */
	protected String getUniqueName() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		StackTraceElement stackTraceElement = elements[2];
		StringBuilder sb = new StringBuilder();
		sb.append(stackTraceElement.getClassName()).append(" > ").append(stackTraceElement.getLineNumber());
		String output = sb.toString();
		return output;
	}

}
