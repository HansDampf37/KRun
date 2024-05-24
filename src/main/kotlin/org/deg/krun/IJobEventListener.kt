package org.deg.krun

/**
 * A JobEventListener can be attached onto a [Job] in order to listen to specific events.
 * @see Job.addEventListener
 * @see onScheduled
 * @see onStarted
 * @see onDone
 * @see onFailure
 *
 * @author Adrian Degenkolb
 */
interface IJobEventListener<I, O> {
    /**
     * Is triggered when the Job is scheduled on a [Scheduler].
     * @param job the [Job] that triggered this event.
     */
    fun onScheduled(job: Job<I, O>) {}

    /**
     * Is triggered right before the Job's [Job.runMethodArg] is invoked.
     * @param job the [Job] that triggered this event.
     * @param input the input arguments for the run method
     */
    fun onStarted(input: I, job: Job<I, O>) {}

    /**
     * Is triggered right after the Job's [Job.runMethodArg] has finished.
     * @param job the [Job] that triggered this event.
     * @param output the output produced by the run method
     * @param input the input arguments for the run method.
     */
    fun onDone(input: I, output: O, job: Job<I, O>) {}

    /**
     * Is triggered when the Job's [Job.runMethodArg] throws an uncaught exception.
     * @param job the [Job] that triggered this event.
     * @param exception the thrown exception
     */
    fun onFailure(exception: Exception, job: Job<I, O>) {}

    /**
     * Is triggered when the Job is canceled by the [Scheduler].
     *
     * @param job the [Job] that triggered this event.
     */
    fun onCancel(job: Job<I, O>) {}
}