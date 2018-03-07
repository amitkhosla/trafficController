package org.ak.trafficController.annotations.api;

/**
 * Task types Enum.
 * Normal tasks are the tasks which are computational tasks. These tasks are not assumed to have any IO call.
 * Slow tasks are considered as tasks which are doing some IO operations.
 * All tasks are executed in executor. There will be limited number of consumers for each type as configured in controlled or submit.
 * The tasks are added in the queue to be picked as soon as an task is free.
 * It is highly advised to divide the tasks in smaller task so that we do not have long running tasks.
 * @author amit.khosla
 */
public enum TaskType {
	NORMAL, SLOW;
}
