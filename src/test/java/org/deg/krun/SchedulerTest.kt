package org.deg.krun

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SchedulerTest {
    @Test
    fun `test that job is scheduled and then started`() {
        var scheduled = false
        var started = false
        val job = Job<Unit, Unit> {}
        job.addEventListener(object : IJobEventListener<Unit, Unit> {
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
    fun `test scheduling with delay`() {
        var scheduled: Long = 0
        var started: Long = 0
        val job = Job<Unit, Unit> {}
        job.addEventListener(object : IJobEventListener<Unit, Unit> {
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
    fun `test future timing out when job takes to long throws TimeoutException`() {
        val job = Job<Unit, Unit> { sleep(10000) }
        try {
            Scheduler.schedule(job, Unit).get(10, TimeUnit.MILLISECONDS)
            fail("Expected a timeout to be thrown")
        } catch (_: TimeoutException) {
        }
    }

    @Test
    fun `test automatic scheduling after another job`() {
        val calculateSomething = Job<Int, Int> { return@Job it * it }
        val printResults = Job<Any, String> { it.toString() }
        val future = Scheduler.scheduleAfter(printResults, calculateSomething)
        Scheduler.schedule(calculateSomething, 3)
        val result = future.get()
        assertEquals("9", result)
    }

    @Test
    fun `test automatic scheduling after another job with output transformation`() {
        val calculateSomething = Job<Int, Int> { return@Job it * it }
        val countSymbols = Job<String, Int> { it.length }
        val future = Scheduler.scheduleAfter(countSymbols, calculateSomething) { it.toString() }
        Scheduler.schedule(calculateSomething, 10)
        val result = future.get()
        assertEquals(3, result)
    }

    @Test
    fun `test automatic scheduling cancels when previous job is canceled`() {
        val slowJob = Job<Unit, Int> {
            sleep(100000)
            return@Job 1
        }
        val dependingJob = Job<Int, Int> { it * it }
        val futureSlowJob = Scheduler.schedule(slowJob)
        val futureDependingJob = Scheduler.scheduleAfter(dependingJob, slowJob)
        futureSlowJob.cancel(true)
        assertTrue(futureDependingJob.isCancelled)
        assertEquals(State.Canceled, dependingJob.state)
    }

    @Test
    fun `test automatic scheduling cancels when previous job fails`() {
        val slowJob = Job<Unit, Int> {
            throw Exception()
        }
        val dependingJob = Job<Int, Int> { it * it }
        val futureDependingJob = Scheduler.scheduleAfter(dependingJob, slowJob)
        Scheduler.schedule(slowJob)
        sleep(30)
        assertTrue(futureDependingJob.isCancelled)
        assertEquals(State.Canceled, dependingJob.state)
    }

    @Test
    fun `test canceling job with future`() {
        var canceled = false
        val cancelListener = object: IJobEventListener<Unit, Int> {
            override fun onCancel(job: Job<Unit, Int>) { canceled = true }
            override fun onDone(input: Unit, output: Int, job: Job<Unit, Int>) {
                fail("Job should not complete because it was canceled")
            }
            override fun onFailure(exception: Exception, job: Job<Unit, Int>) {
                fail("Job should not complete because it was canceled")
            }
        }

        val job = Job(jobEventListener = cancelListener) {
            sleep(10000)
            return@Job 0
        }
        val future = job.schedule()
        val success = future.cancel(true)
        assertTrue(success)
        assertTrue(canceled)
        assertEquals(State.Canceled, job.state)
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun cleanUp() {
            Scheduler.shutdown()
        }
    }
}