package org.deg.krun

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SchedulerTest {
    @Test
    fun testJobIsScheduledAndThenStarted() {
        var scheduled = false
        var started = false
        val job = Job<Unit, Unit> {}
        job.addEventListener(object: IJobEventListener<Unit, Unit> {
            override fun onScheduled(job: Job<Unit, Unit>) {
                assertFalse(scheduled)
                assertFalse(started)
                scheduled = true
            }

            override fun onStarted(input: Unit, job: Job<Unit, Unit>) {
                assertTrue(scheduled)
                assertFalse(started)
                started = true
            }
        })
        Scheduler.schedule(job, Unit).get()
        assertTrue(scheduled && started)
    }

    @Test
    fun testScheduleWithDelay() {
        var scheduled: Long = 0
        var started: Long = 0
        val job = Job<Unit, Unit> {}
        job.addEventListener(object: IJobEventListener<Unit, Unit> {
            override fun onScheduled(job: Job<Unit, Unit>) {
                scheduled = System.currentTimeMillis()
            }

            override fun onStarted(input: Unit, job: Job<Unit, Unit>) {
                started = System.currentTimeMillis()
            }
        })
        val delay: Long = 1000
        val epsilon = 10
        Scheduler.schedule(job, Unit, delay, TimeUnit.MILLISECONDS).get()
        assertTrue(started - scheduled > delay - epsilon)
    }

    @Test
    fun testTimeout() {
        val job = Job<Unit, Unit> {sleep(10000)}
        try {
            Scheduler.schedule(job, Unit).get(10, TimeUnit.MILLISECONDS)
            fail("Expected a timeout to be thrown")
        } catch (_: TimeoutException) {}
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun cleanUp() {
            Scheduler.shutdown()
        }
    }
}