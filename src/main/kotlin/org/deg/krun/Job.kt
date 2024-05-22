package org.deg.krun

import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A Job implements some runnable behavior in its [runMethod] with defined input-type [I] and output-type [O].
 * The job can be run directly by using [Job.run] or can be scheduled on a [Scheduler] by using [Job.schedule] for asynchronous invocation.
 * [IJobEventListener] can be attached to the job in order to react to events asynchronously (either directly by setting the property in
 * the constructor or indirectly by invoking [addEventListener]).
 *
 *  @author Adrian Degenkolb
 */
open class Job<I, O>(
    val name: String = "job-${UUID.randomUUID()}",
    jobEventListener: IJobEventListener<I, O>? = null,
    private val runMethod: (input: I) -> O
) {
    /**
     * The current [JobStatus] describes the current state of the Job. For example, it could be [JobStatus.Running].
     */
    var status: JobStatus = JobStatus.Inactive
        private set
    private val jobEventListeners: MutableList<IJobEventListener<I, O>> = ArrayList()
    private var output: O? = null
    private var finishedLock = Object()

    init {
        if (jobEventListener != null) addEventListener(jobEventListener)
    }

    /**
     * Runs the job.
     * @param input input of generic type [I]
     * @return output of generic type [O]
     */
    open fun run(input: I): O {
        onStarted(input)
        try {
            val output: O = runMethod(input)
            onDone(input, output)
            return output
        } catch (e: Exception) {
            onFailure(e)
            throw e
        }
    }

    /**
     * Adds a new [IJobEventListener] to this job.
     * @param jobEventListener the new listener
     * @return handler of the event listener
     */
    fun addEventListener(jobEventListener: IJobEventListener<I, O>): Int {
        jobEventListeners.add(jobEventListener)
        return jobEventListener.hashCode()
    }

    /**
     * Removes an existing [IJobEventListener] from this job.
     * @param handler the handler of the event listener is returned by [addEventListener].
     * @return true if a JobEventListener was removed, false otherwise.
     */
    fun removeEventListener(handler: Int): Boolean {
        val el = jobEventListeners.find { it.hashCode() == handler } ?: return false
        jobEventListeners.remove(el)
        return true
    }

    /**
     * Returns and removes all [IJobEventListener]s from this job.
     * @return the returned [IJobEventListener]
     */
    fun removeAllEventListeners(): List<IJobEventListener<I, O>> {
        val listeners = jobEventListeners.toList()
        jobEventListeners.clear()
        return listeners
    }

    /**
     * @return a [Future] that can be used to await this jobs termination and to get the output.
     */
    fun getFuture(): Future<O> {
        return object : Future<O> {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

            override fun isCancelled(): Boolean = status == JobStatus.Canceled

            override fun isDone(): Boolean = status == JobStatus.Done

            override fun get(): O {
                waitForTermination()
                return output!!
            }

            override fun get(timeout: Long, unit: TimeUnit): O {
                waitForTermination(timeout, unit)
                return output!!
            }
        }
    }

    /**
     * Trigger this job automatically after another job has finished.
     * @param previousJob the job after which this job is triggered
     * @param block transforms the output of the [previousJob] to a fitting input-format for this job
     * @return a [Future] for this job
     */
    open fun <I1, O1> triggerAfter(previousJob: Job<I1, O1>, block: (output: O1) -> I): Future<O> {
        val jobEventListener: IJobEventListener<I1, O1> = object : IJobEventListener<I1, O1> {
            override fun onDone(input: I1, output: O1, job: Job<I1, O1>) {
                val convertedOutput = block(output)
                Scheduler.schedule(this@Job, convertedOutput)
            }
        }
        previousJob.addEventListener(jobEventListener)
        return getFuture()
    }

    /**
     * Trigger this job automatically after another job has finished and use the previous jobs output as input for
     * this job.
     * @param previousJob the job after which this job is triggered
     * @return a [Future] for this job
     */
    open fun <I1 : I> triggerAfter(previousJob: Job<*, I1>) = triggerAfter(previousJob, Utils::identity)

    /**
     * Waits until the job is complete.
     * If the specified timeout is reached a [TimeoutException] is thrown.
     *
     * @param timeout the duration
     * @param unit the unit of the [timeout]
     * @throws TimeoutException if the timeout is exceeded
     */
    fun waitForTermination(timeout: Long = -1, unit: TimeUnit = TimeUnit.MILLISECONDS) {
        synchronized(finishedLock) {
            val timeoutMillis = unit.toMillis(timeout)
            val endTime = System.currentTimeMillis() + timeoutMillis
            while (output == null) {
                val remainingTime = endTime - System.currentTimeMillis()
                if (timeout > 0 && remainingTime <= 0) {
                    throw TimeoutException("Awaiting job $name timed out after $timeout $unit")
                }
                try {
                    if (timeout > 0) finishedLock.wait(remainingTime)
                    else finishedLock.wait()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }


    protected open fun onStarted(input: I) {
        status = JobStatus.Running
        jobEventListeners.forEach {
            try {
                it.onStarted(input, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected open fun onDone(input: I, output: O) {
        status = JobStatus.Done
        this.output = output
        synchronized(finishedLock) {
            finishedLock.notifyAll()
        }
        jobEventListeners.forEach {
            try {
                it.onDone(input, output, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal open fun onScheduled() {
        status = JobStatus.Scheduled
        jobEventListeners.forEach {
            try {
                it.onScheduled(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected open fun onFailure(e: Exception) {
        status = JobStatus.Failed
        jobEventListeners.forEach {
            try {
                it.onFailure(e, this)
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }
    }

    companion object {
        fun <T, S : T> identity(lastJobsOutput: S): T {
            return lastJobsOutput
        }
    }
}