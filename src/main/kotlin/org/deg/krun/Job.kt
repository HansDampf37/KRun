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
    /**
     * The current [State] describes the current state of the Job. For example, it could be [State.Running].
     */
    var status: State = State.Inactive
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

    protected open fun onStarted(input: I) {
        status = State.Running
        jobEventListeners.forEach {
            try {
                it.onStarted(input, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected open fun onDone(input: I, output: O) {
        status = State.Done
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
        status = State.Scheduled
        jobEventListeners.forEach {
            try {
                it.onScheduled(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected open fun onFailure(e: Exception) {
        status = State.Failed
        jobEventListeners.forEach {
            try {
                it.onFailure(e, this)
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }
    }

    internal open fun onCancel() {
        status = State.Canceled
        jobEventListeners.forEach {
            try {
                it.onCancel(this)
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }
    }
}

/**
 * Invokes [Job.run] without inputs
 */
fun <O> Job<Unit, O>.run() = run(Unit)