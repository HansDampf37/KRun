package org.deg.krun

import java.lang.Thread.sleep
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

object Scheduler {
    private val threadPool = Executors.newCachedThreadPool()

    fun <I, O> schedule(job: Job<I, O>, args: I, delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O?> {
        return threadPool.submit(Callable {
            val delayInMs = TimeUnit.MILLISECONDS.convert(delay, unit)
            sleep(delayInMs)
            job.run(args)
        })
    }

    fun shutdown() {
        threadPool.shutdownNow()
    }
}