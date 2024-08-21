# KRun
A lightweight and flexible framework for jobs and scheduling.

## Usage
KRun allows you to create Job objects that can execute a specific function.

### Creating Jobs
Here we create a `Job` where both the input type and output type are `Unit`. Then we run the job, which results in a 1-second delay followed by writing "finished" to `stdout`.
```kotlin
val job = Job<Unit, Unit> {
    sleep(1000)
    println("finished")
}
job.run()
```
Another example:
```kotlin
val sumUntil = Job<Int, Int> { input: Int ->
    return@Job input * (input + 1) / 2
}
val sumUntil100: Int = sumUntil.run(100)
```
More complex jobs can also be encapsulated in a new class that inherits from `Job` and overrides its `runMethod`.
```kotlin
class DownloadJob: Job<Unit, File>() {
    override fun runMethod(input: Unit): File {
        TODO("Implement run behavior")
    }
}
```

### Listening to Events
Jobs trigger certain events, such as the `onFailure` event. We can listen to these events and react accordingly.
```kotlin
val jobEventListener = object : IJobEventListener<Unit, Unit> {
    override fun onFailure(exception: Exception, job: Job<Unit, Unit>) {
        println("Job ${job.name} threw an exception of type ${exception::class}")
    }
}
val job = Job(jobEventListener = jobEventListener) {
    throw Exception("Oops, something went wrong!?")
}
job.run()
```
Here, we create an `IJobEventListener` that overrides the `onFailure` function. This event listener is then added to the Job via the Job's constructor (we could have also used the `addEventListener` method). Invoking `job.run()` leads to an exception being thrown, which triggers the `onFailure` event. Please note that the `job.run()` method will still throw the exception. `IJobEventListeners` do not replace adequate exception handling.

### Running Jobs Asynchronously
So far, everything we did was executed sequentially on a single thread. The `Scheduler` offers the possibility to schedule jobs asynchronously.
```kotlin
val scheduler = Scheduler()
val job1 = Job<Unit, Unit> {
    repeat(3) {
        sleep(1000)
        println("Job 1 working...")
    }
}
val job2 = Job<Unit, Unit> {
    repeat(3) {
        sleep(1000)
        println("Job 2 working...")
    }
}
val futureJob1 = scheduler.schedule(job1)
val futureJob2 = scheduler.schedule(job2)
futureJob1.get()
futureJob2.get()
scheduler.shutdown()
```
Now we have two jobs. As both operations are complex, the calculations tend to take time, and it would be inefficient to run them sequentially. However, we can also schedule them on a scheduler for asynchronous execution and then wait for the returned futures to finish. Remember to always shut down the Scheduler's threads by either invoking `exitProcess` or `Scheduler.shutdown`. Similar to the `run` method, the `schedule` method also accepts arguments.
```kotlin
val future = scheduler.schedule(job, input)
```

### Chaining Jobs Together
The `Scheduler` also provides the possibility to chain jobs together. The output of the first job is then used as input for the second job.
```kotlin
val scheduler = Scheduler()
val downloadJob = Job<Unit, File> {
    val file = File("path")
    file.writeText("Downloaded content")
    file
}
val processingDownload = Job<File, Unit> {
    println(it.readText())
}
// Equivalent: val futureForProcessingDownload = processingDownload + downloadJob
val futureForProcessingDownload = scheduler.scheduleAfter(processingDownload, downloadJob)
scheduler.schedule(downloadJob)
futureForProcessingDownload.get()
scheduler.shutdown()
```
By specifying a `transform` method, we can chain jobs with arbitrary input and output types together.
```kotlin
val scheduler = Scheduler()
val downloadJob = Job<String, File> {
    File(it)
}
val countLines = Job<String, Int> {
    it.lines().size
}
val futureForProcessingDownload = scheduler.scheduleAfter(countLines, downloadJob) {
    it.readText()
}
scheduler.schedule(downloadJob, "README.md")
val amountOfLines = futureForProcessingDownload.get()
println("Readme has $amountOfLines lines")
scheduler.shutdown()
```
Note that the output type of the `downloadJob` (`File`) does not equal the input type of the `countLines` job (`String`). The specified `transform` method transforms `File` to `String`. The overall result is:
````text
Readme has 117 lines
