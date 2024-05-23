package org.deg.krun

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

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
                assertEquals(State.Running, job.status)
                started = true
            }

            override fun onDone(input: Int, output: String, job: Job<Int, String>) {
                assertTrue(started)
                assertFalse(done)
                assertEquals(State.Done, job.status)
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
                assertEquals(State.Running, job.status)
                started = true
            }

            override fun onFailure(exception: Exception, job: Job<Unit, Nothing>) {
                assertTrue(started)
                assertFalse(failed)
                assertEquals(State.Failed, job.status)
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
