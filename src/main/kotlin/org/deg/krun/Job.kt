package org.deg.krun

import java.util.*

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
    private val jobEventListeners: MutableList<IJobEventListener<I, O>> = ArrayList()

    init {
        if (jobEventListener != null) addEventListener(jobEventListener)
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

    private fun onStarted(input: I) {
        jobEventListeners.forEach {
            try {
                it.onStarted(input, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onDone(input: I, output: O) {
        jobEventListeners.forEach {
            try {
                it.onDone(input, output, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal fun onScheduled() {
        jobEventListeners.forEach {
            try {
                it.onScheduled(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onFailure(e: Exception) {
        jobEventListeners.forEach {
            try {
                it.onFailure(e, this)
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }
    }
}