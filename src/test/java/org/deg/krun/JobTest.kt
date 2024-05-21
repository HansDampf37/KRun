package org.deg.krun

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JobTest {
    private var job = Job<Int, String> {return@Job it.toString()}

    @BeforeEach
    fun setup() {
        job = Job{return@Job it.toString()}
    }

    @Test
    fun addEventListener() {
        job.addEventListener(object: JobEventListener<Int, String> {})
        assertEquals(1, countJobEventListeners(job))
    }

    @Test
    fun removeEventListener() {
        val handler = job.addEventListener(object: JobEventListener<Int, String> {})
        job.removeEventListener(handler)
        assertEquals(0, countJobEventListeners(job))
    }

    @Test
    fun removeAllEventListeners() {
        job.addEventListener(object: JobEventListener<Int, String> {})
        job.addEventListener(object: JobEventListener<Int, String> {})
        job.addEventListener(object: JobEventListener<Int, String> {})
        job.removeAllEventListeners()
        assertEquals(0, countJobEventListeners(job))
    }

    @Test
    fun getName() {
        assertTrue(job.name.isNotEmpty())
    }

}

private fun countJobEventListeners(job: Job<*, *>): Int {
    val jobEventListenersField = job::class.java.getDeclaredField("jobEventListeners")
    jobEventListenersField.isAccessible = true
    val jobEventListeners = jobEventListenersField.get(job) as List<*>
    return jobEventListeners.size
}
