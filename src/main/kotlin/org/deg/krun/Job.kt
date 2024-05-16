package org.deg.krun

import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * A Job implements some runnable behavior in its [runMethod] with defined input-type [I] and output-type [O].
 * The job can be run directly by using [Job.run] or can be scheduled on a [Scheduler] by using [Job.schedule] for asynchronous invocation.
 * [JobEventListener] can be attached to the job in order to react to events asynchronously (either directly by setting the property in
 * the constructor or indirectly by invoking [addEventListener].
 *
 *  @author Adrian Degenkolb
 */
open class Job<I, O>(
    val name: String = "job-${UUID.randomUUID().toString()}",
    jobEventListener: JobEventListener<I, O>? = null,
    private val runMethod: (input: I) -> O
) {
    private val jobEventListeners: MutableList<JobEventListener<I, O>> = ArrayList()

    init {
        if (jobEventListener != null) addEventListener(jobEventListener)
    }

    /**
     * Adds a new [JobEventListener] to this job.
     * @param jobEventListener the new listener
     */
    fun addEventListener(jobEventListener: JobEventListener<I, O>) {
        jobEventListeners.add(jobEventListener)
    }

    /**
     * Runs the job.
     * @param input input of generic type [I]
     * @return output of generic type [O]
     */
    fun run(input: I): O {
        onStarted(input)
        try {
            val output: O = runMethod(input)
            onDone(output)
            return output
        } catch (e: Exception) {
            jobEventListeners.forEach {
                try {
                    it.onFailure(e, this)
                } catch (e1: Exception) {
                    e1.printStackTrace()
                }
            }
            throw e
        }
    }

    /**
     * Schedules this Job on a [Scheduler] for asynchronous execution.
     * @param input input arguments for the job's [run method][Job.run]
     * @param delay delay after which the job is executed
     * @param unit timeunit for the delay
     * @return Future containing the output of the [run method][Job.run]
     */
    fun schedule(input: I, delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O?> {
        onScheduled()
        return Scheduler.schedule(this, input, delay, unit)
    }

    private fun onStarted(input: I) {
        jobEventListeners.forEach {
            try {
                it.onStarted(input, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onDone(output: O) {
        jobEventListeners.forEach {
            try {
                it.onDone(output, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onScheduled() {
        jobEventListeners.forEach {
            try {
                it.onScheduled(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}