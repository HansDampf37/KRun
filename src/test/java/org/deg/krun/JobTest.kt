package org.deg.krun

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.lang.IllegalStateException
import java.lang.Thread.sleep
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class JobTest {
    private var job = Job<Int, String> { return@Job it.toString() }

    @BeforeEach
    fun setup() {
        job = Job { return@Job it.toString() }
    }

    @Test
    fun addEventListener() {
        job.addEventListener(object : IJobEventListener<Int, String> {})
        assertEquals(1, countJobEventListeners(job))
    }

    @Test
    fun removeEventListener() {
        val handler = job.addEventListener(object : IJobEventListener<Int, String> {})
        job.removeEventListener(handler)
        assertEquals(0, countJobEventListeners(job))
    }

    @Test
    fun removeAllEventListeners() {
        job.addEventListener(object : IJobEventListener<Int, String> {})
        job.addEventListener(object : IJobEventListener<Int, String> {})
        job.addEventListener(object : IJobEventListener<Int, String> {})
        job.removeAllEventListeners()
        assertEquals(0, countJobEventListeners(job))
    }

    @Test
    fun getName() {
        assertTrue(job.name.isNotEmpty())
    }

    @Test
    fun testRun() {
        var started = false
        var done = false
        job.addEventListener(object : IJobEventListener<Int, String> {
            override fun onStarted(input: Int, job: Job<Int, String>) {
                assertFalse(started)
                assertFalse(done)
                assertEquals(State.Running, job.state)
                started = true
            }

            override fun onDone(input: Int, output: String, job: Job<Int, String>) {
                assertTrue(started)
                assertFalse(done)
                assertEquals(State.Done, job.state)
                done = true
            }
        })
        val result = job.run(1)
        assertTrue(started && done)
        assertEquals("1", result)
    }

    @Test
    fun `test run on subclass of job`() {
        var started = false
        var done = false
        val eventListener = object : IJobEventListener<Int, String> {
            override fun onStarted(input: Int, job: Job<Int, String>) {
                assertFalse(started)
                assertFalse(done)
                assertEquals(State.Running, job.state)
                started = true
            }

            override fun onDone(input: Int, output: String, job: Job<Int, String>) {
                assertTrue(started)
                assertFalse(done)
                assertEquals(State.Done, job.state)
                done = true
            }
        }
        val result = object: Job<Int, String>(jobEventListener = eventListener) {
            override fun runMethod(input: Int): String {
                return input.toString()
            }
        }.run(1)
        assertTrue(started && done)
        assertEquals("1", result)
    }

    @Test
    fun `test undefined run behaviour triggers exception`() {
        try {
            Job<Unit, Unit>().run()
            fail("Expected an ${IllegalStateException::class.java.simpleName} to be thrown")
        } catch (_: IllegalStateException) { }
    }

    @Test
    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION", "UNREACHABLE_CODE")
    fun testFailingRun() {
        val failJob = Job<Unit, Nothing> {
            throw Exception("")
        }
        var started = false
        var failed = false
        failJob.addEventListener(object : IJobEventListener<Unit, Nothing> {
            override fun onStarted(input: Unit, job: Job<Unit, Nothing>) {
                assertFalse(started)
                assertFalse(failed)
                assertEquals(State.Running, job.state)
                started = true
            }

            override fun onFailure(exception: Exception, job: Job<Unit, Nothing>) {
                assertTrue(started)
                assertFalse(failed)
                assertEquals(State.Failed, job.state)
                failed = true
            }
        })
        try {
            failJob.run()
            fail(message = "The job should fail and go into the catch statement")
        } catch (e: Throwable) {
            assertTrue(started && failed)
        }
    }

    @Test
    fun `test contents of statistic`() {
        val epsilon = 30L
        val jobDuration = 1000L
        val schedulingDelay = 200L
        val job = Job<Unit, Unit> {
            sleep(jobDuration)
        }
        Scheduler.schedule(job, schedulingDelay, TimeUnit.MILLISECONDS).get()
        assertNull(job.statistic.canceledAt)
        assertNull(job.statistic.failedAt)
        assertNotNull(job.statistic.scheduledAt)
        assertNotNull(job.statistic.startedAt)
        assertNotNull(job.statistic.succeededAt)
        assertNotNull(job.statistic.duration)
        assertTrue(abs(job.statistic.duration!!.toMillis() - jobDuration) < epsilon)
        assertTrue(
            abs(
                Duration.between(job.statistic.scheduledAt, job.statistic.startedAt).toMillis() - schedulingDelay
            ) < epsilon
        )
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun cleanUp() {
            //Scheduler.shutdown()
        }
    }
}

private fun countJobEventListeners(job: Job<*, *>): Int {
    val jobEventListenersField = job::class.java.getDeclaredField("jobEventListeners")
    jobEventListenersField.isAccessible = true
    val jobEventListeners = jobEventListenersField.get(job) as List<*>
    return jobEventListeners.size
}
