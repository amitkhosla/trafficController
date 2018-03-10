# trafficController
This project deals with different aspects of tasks management like object pooling, event based programming, in memory queues.


# About the project
This project can be divided in following subprojects

# a. Tasks
If we see different eco systems for example any java script application or Node JS server or few distributed cache stores/ data stores like Redis, we can see, they work on single threaded model. Benefit of single threaded model is that we do not need to worry about context switching.<br>
If we think about anything we do as a task, any flow can be broken in series of very small tasks. Consider a system where we want our system to be highly effective in terms of average response time, these kind of infra helps to proceed further. Having many threads <b>working together</b> can make system response slowly. Some tasks might be actually waiting because of some I/O calls etc. which is not allowing us to have full utilization of the CPU.<br>
Thus, if we have many small tasks and any I/O kind of tasks are considered slow and all computational tasks considered as normal tasks can help us build an eco system where we can utilize our CPU cycles. Where computational tasks getting more space, slow tasks are running in parallel to return output or submit some data to remote system.<br>
This project will definitely help for such tasks. But for doing so, we need to be think more in terms of functional programming.<br>
This part is the most important use case to actually start this project. The others can help in many cases but this can help in a lot many cases.<br>
To use this, one can just add an Annotation on their current methods and we can switch to this. Though we can think of further improvement in case we directly call the framework.<br>

# b. In Memory Queue
There are many reasons where one components need to communicate to other component. At a particular time one might also think to work on some processing done on some objects in seperate thread without affecting current flow. <br>
For example, I want to do some processing of auditing calculations post persistence of my object. For this, I do not really need to wait my actual thread, I will add this object to my in memory queue where consumer of it will make the necessary calculation.<br>
Another example can be logging of messages. The messages logging can be added to queue whose consumer can write it on file as per the configured settings.<br>
Using in memory queues can be very useful to implement parallel processing. Many systems now a days work on parallel on event based model. Consider a system where a request lands which is queued in a queue and these request process certain types of processing say we have some component working on database and there is another component which is post performing operation on database sends it to some nosql database. Thus, we can have three queues where one's consumer is processing and sending result to next queue. (Obviously we are talking about async operations here and not sync operation.

# c. Object pool
There are many reasons one can think of creating a pool. Creation of an object can be very expensive. Creation of simple objects is also considerd as expensive operation. Thus having a pool is highly advisable.<br>
There are many areas in our applications which are being created for short time and then we need to create new. So, to take benefit of not needed to create so many objects and ask GC to clean, we can have small pool having these objects reset to just like what we expect from new.<br>
This pool can be utilized by implementing inteface <b>Poolable</b> and in clean method, clean the object to make it as good as new. The clean method will be called in seprate thread, so the actual flow will not be impacted as such.
