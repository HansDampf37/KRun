package org.deg.krun

import java.lang.Thread.sleep
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * The Scheduler schedules [Jobs][Job] for asynchronous execution.
 * @see schedule
 *  @author Adrian Degenkolb
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
    fun <I, O> schedule(job: Job<I, O>, input: I, delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O?> {
        return threadPool.submit(Callable {
            val delayInMs = TimeUnit.MILLISECONDS.convert(delay, unit)
            sleep(delayInMs)
            job.run(input)
        })
    }

    /**
     * Shuts down the Scheduler.
     */
    fun shutdown() {
        threadPool.shutdownNow()
    }

    init {
        Runtime.getRuntime().addShutdownHook(object: Thread() {
            override fun run() {
                shutdown()
            }
        })
    }
}