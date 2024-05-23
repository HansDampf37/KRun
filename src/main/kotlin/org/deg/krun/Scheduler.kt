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

    /**
     * Schedules a Job on this [Scheduler] for asynchronous execution.
     * @param job the job that should be scheduled
     * @param delay delay after which the job is executed
     * @param unit timeunit for the delay
     * @return Future containing the output of the job's [run method][Job.run]
     */
    fun <O> schedule(job: Job<Unit, O>, delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O> {
        return schedule(job, Unit, delay, unit)
    }

    /**
     * Schedule this job automatically after another job has finished.
     *
     * @param laterJob the job to be scheduled
     * @param previousJob the job after which this job is triggered
     * @param delay delay between completion of [previousJob] and start of [laterJob]
     * @param unit unit of the delay
     * @param transform transforms the output of the [previousJob] to a fitting input-format for this job
     * @return a [Future] for this job
     */
    fun <I, O, I1, O1>scheduleAfter(
        laterJob: Job<I, O>,
        previousJob: Job<I1, O1>,
        delay: Long = 0,
        unit: TimeUnit = TimeUnit.SECONDS,
        transform: (O1) -> I
    ): Future<O> {
        if (arrayOf(State.Canceled, State.Done, State.Failed).contains(previousJob.state)) {
            throw IllegalArgumentException("Job $laterJob cannot be scheduled after job $previousJob because previous job is already finished")
        }

        var jobInput: I? = null
        val wakeUpLock = Object()

        val wakeUpJob = Job<Unit, O>(name="Wake-Up-Job for ${laterJob.name}") {
            synchronized(wakeUpLock) {
                wakeUpLock.wait()
            }
            schedule(laterJob, jobInput!!, delay, unit).get()
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
                laterJob.onCancel()
                future.cancel(true)
            }
        })

        return future
    }

    /**
     * Schedule this job automatically after another job has finished. Uses the output of [previousJob] as input for
     * [laterJob]
     *
     * @param laterJob the job to be scheduled
     * @param previousJob the job after which this job is triggered
     * @param delay delay between completion of [previousJob] and start of [laterJob]
     * @param unit unit of the delay
     * @return a [Future] for this job
     */
    fun <I, O, I1, O1 : I>scheduleAfter(
        laterJob: Job<I, O>,
        previousJob: Job<I1, O1>,
        delay: Long = 0,
        unit: TimeUnit = TimeUnit.SECONDS,
    ) = scheduleAfter(laterJob, previousJob, delay, unit, Utils::identity)

    /**
     * Shuts down the Scheduler.
     */
    fun shutdown(timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS) {
        if (timeout == 0L) threadPool.shutdown()
        else threadPool.awaitTermination(timeout, unit)
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

operator fun <I1, O1: I2, I2, O2> Job<I1, O1>.plus(other: Job<I2, O2>) = Scheduler.scheduleAfter(other, this)