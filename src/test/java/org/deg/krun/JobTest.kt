package org.deg.krun

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class JobTest {
    private var job = Job<Int, String> {return@Job it.toString()}

    @BeforeEach
    fun setup() {
        job = Job{return@Job it.toString()}
    }

    @Test
    fun addEventListener() {
        job.addEventListener(object: IJobEventListener<Int, String> {})
        assertEquals(1, countJobEventListeners(job))
    }

    @Test
    fun removeEventListener() {
        val handler = job.addEventListener(object: IJobEventListener<Int, String> {})
        job.removeEventListener(handler)
        assertEquals(0, countJobEventListeners(job))
    }

    @Test
    fun removeAllEventListeners() {
        job.addEventListener(object: IJobEventListener<Int, String> {})
        job.addEventListener(object: IJobEventListener<Int, String> {})
        job.addEventListener(object: IJobEventListener<Int, String> {})
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
        job.addEventListener(object: IJobEventListener<Int, String> {
            override fun onStarted(input: Int, job: Job<Int, String>) {
                assertFalse(started)
                assertFalse(done)
                assertEquals(JobStatus.Running, job.status)
                started = true
            }

            override fun onDone(input: Int, output: String, job: Job<Int, String>) {
                assertTrue(started)
                assertFalse(done)
                assertEquals(JobStatus.Done, job.status)
                done = true
            }
        })
        val result = job.run(1)
        assertTrue(started && done)
        assertEquals("1",  result)
    }

    @Test
    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION", "UNREACHABLE_CODE")
    fun testFailingRun() {
        val failJob = Job<Unit, Nothing> {
            throw Exception("")
        }
        var started = false
        var failed = false
        failJob.addEventListener(object: IJobEventListener<Unit, Nothing> {
            override fun onStarted(input: Unit, job: Job<Unit, Nothing>) {
                assertFalse(started)
                assertFalse(failed)
                assertEquals(JobStatus.Running, job.status)
                started = true
            }

            override fun onFailure(exception: Exception, job: Job<Unit, Nothing>) {
                assertTrue(started)
                assertFalse(failed)
                assertEquals(JobStatus.Failed, job.status)
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
    fun testPipe() {
        val calculateSomething = Job<Int, Int> { return@Job it * it }
        val printResults = Job<Any, String> { it.toString() }
        val future = printResults.triggerAfter(calculateSomething)
        calculateSomething.run(3)
        val result = future.get()
        assertEquals("9", result)
    }

    @Test
    fun testPipe2() {
        val calculateSomething = Job<Int, Int> { return@Job it * it }
        val countSymbols = Job<String, Int> { it.length }
        val future = countSymbols.triggerAfter(calculateSomething) { it.toString() }
        calculateSomething.run(10)
        val result = future.get()
        assertEquals(3, result)
    }

    @Test
    fun testGetFuture() {
        val job = Job<Unit, Int> {
            sleep(1000)
            return@Job 1
        }
        val future = job.getFuture()
        Scheduler.schedule(job, Unit)
        try {
            val result = future.get(10, TimeUnit.MILLISECONDS)
            fail("Expected a timeout to be thrown, instead got result $result")
        } catch (_: TimeoutException) { }
        val result = future.get()
        assertEquals(1, result)
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun cleanUp() {
            Scheduler.shutdown()
        }
    }
}

private fun countJobEventListeners(job: Job<*, *>): Int {
    val jobEventListenersField = job::class.java.getDeclaredField("jobEventListeners")
    jobEventListenersField.isAccessible = true
    val jobEventListeners = jobEventListenersField.get(job) as List<*>
    return jobEventListeners.size
}
