package org.deg.krun

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

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
}