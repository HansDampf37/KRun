package org.deg.krun

import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

open class Job<I, O>(
    val name: String = "job-${UUID.randomUUID().toString()}",
    jobEventListener: JobEventListeners<I, O>? = null,
    private val runMethod: (input: I) -> O
) {
    private val jobEventListeners: MutableList<JobEventListeners<I, O>> = ArrayList()

    init {
        if (jobEventListener != null) addEventListener(jobEventListener)
    }

    fun addEventListener(jobEventListener: JobEventListeners<I, O>) {
        jobEventListeners.add(jobEventListener)
    }

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

    fun schedule(input: I, delay: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<O?> {
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
}