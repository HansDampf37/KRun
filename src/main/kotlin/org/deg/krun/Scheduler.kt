package org.deg.krun

import java.lang.IllegalArgumentException
import java.lang.Thread.sleep
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * The Scheduler schedules [Jobs][Job] for asynchronous execution.
 * This Scheduler needs to be shutdown by either using the [shutdown]-method or [System.exit]
 *
 * @see schedule
 * @author Adrian Degenkolb
 */
object Scheduler {
    private val threadPool = Executors.newCachedThreadPool()

    /**
     * Schedules a Job on this [Scheduler] for asynchronous execution.
     * @param job the job that should be scheduled
     * @param input input arguments for the job's [run method][Job.run]
     * @param delay delay after which the job is executed
     * @param unit timeunit for the delay
     * @return Future containing the output of the job's [run method][Job.run]
     */
    fun <I, O> schedule(job: Job<I, O>, input: I, delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O> {
        job.onScheduled()
        val future = threadPool.submit(Callable {
            val delayInMs = TimeUnit.MILLISECONDS.convert(delay, unit)
            sleep(delayInMs)
            job.run(input)
        })
        return object: Future<O> {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                if (future.cancel(mayInterruptIfRunning)) {
                    job.onCancel()
                    return true
                }
                return false
            }
            override fun isCancelled(): Boolean = future.isCancelled
            override fun isDone(): Boolean = future.isCancelled
            override fun get(): O = future.get()
            override fun get(timeout: Long, unit: TimeUnit): O = future.get(timeout, unit)
        }
    }

    fun <O> schedule(job: Job<Unit, O>, delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O> {
        return schedule(job, Unit, delay, unit)
    }

    /**
     * Schedule this job automatically after another job has finished.
     *
     * @param job the job to be scheduled
     * @param previousJob the job after which this job is triggered
     * @param delay delay between completion of [previousJob] and start of [job]
     * @param unit unit of the delay
     * @param transform transforms the output of the [previousJob] to a fitting input-format for this job
     * @return a [Future] for this job
     */
    fun <I, O, I1, O1>scheduleAfter(
        job: Job<I, O>,
        previousJob: Job<I1, O1>,
        delay: Long = 0,
        unit: TimeUnit = TimeUnit.SECONDS,
        transform: (O1) -> I
    ): Future<O> {
        if (arrayOf(JobStatus.Canceled, JobStatus.Done, JobStatus.Failed).contains(previousJob.status)) {
            throw IllegalArgumentException("Job $job cannot be scheduled after job $previousJob because previous job is already finished")
        }

        var jobInput: I? = null
        val wakeUpLock = Object()

        val wakeUpJob = Job<Unit, O>(name="Wake-Up-Job for ${job.name}") {
            synchronized(wakeUpLock) {
                wakeUpLock.wait()
            }
            schedule(job, jobInput!!, delay, unit).get()
        }
        val future = schedule(wakeUpJob)

        previousJob.addEventListener(object: IJobEventListener<I1, O1> {
            override fun onDone(input: I1, output: O1, job: Job<I1, O1>) {
                jobInput = transform(output)
                synchronized(wakeUpLock) {
                    wakeUpLock.notifyAll()
                }
            }

            override fun onFailure(exception: Exception, job: Job<I1, O1>) {
                onCancel(job)
            }

            override fun onCancel(job: Job<I1, O1>) {
                future.cancel(true)
            }
        })

        return future
    }

    /**
     * Schedule this job automatically after another job has finished. Uses the output of [previousJob] as input for
     * [job]
     *
     * @param job the job to be scheduled
     * @param previousJob the job after which this job is triggered
     * @param delay delay between completion of [previousJob] and start of [job]
     * @param unit unit of the delay
     * @return a [Future] for this job
     */
    fun <I, O, I1, O1 : I>scheduleAfter(
        job: Job<I, O>,
        previousJob: Job<I1, O1>,
        delay: Long = 0,
        unit: TimeUnit = TimeUnit.SECONDS,
    ) = scheduleAfter(job, previousJob, delay, unit, Utils::identity)

    /**
     * Shuts down the Scheduler.
     */
    fun shutdown(timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS) {
        threadPool.awaitTermination(timeout, unit)
    }

    init {
        Runtime.getRuntime().addShutdownHook(object: Thread() {
            override fun run() {
                shutdown(2000)
            }
        })
    }
}

/**
 * Schedules this Job on a [Scheduler] for asynchronous execution.
 * @param input input arguments for the job's [run method][Job.run]
 * @param delay delay after which the job is executed
 * @param unit timeunit for the delay
 * @return Future containing the output of the [run method][Job.run]
 */
fun <I, O> Job<I, O>.schedule(input: I, delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O> {
    return Scheduler.schedule(this, input, delay, unit)
}

/**
 * Invoke [Job.schedule] with Unit as input
 */
fun <O> Job<Unit, O>.schedule(delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O> = schedule(Unit, delay, unit)