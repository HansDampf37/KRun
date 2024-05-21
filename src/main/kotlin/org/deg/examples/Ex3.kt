package org.deg.examples

import org.deg.krun.Job
import org.deg.krun.IJobEventListener
import org.deg.krun.schedule
import java.lang.Thread.sleep
import kotlin.system.exitProcess

fun main() {
    val jobEventListener = object : IJobEventListener<Unit, Int> {
        override fun onStarted(input: Unit, job: Job<Unit, Int>) = println("started ${job.name}")
        override fun onDone(input: Unit, output: Int, job: Job<Unit, Int>) = println("completed ${job.name}")
    }

    // define multiple slow jobs that should be run in parallel
    val jobs = List(3) { i ->
        Job(jobEventListener = jobEventListener) { _: Unit ->
            println("complex calculations")
            val processingDuration = ((Math.random() + 1) * 1000L).toLong()
            sleep(processingDuration)
            return@Job i
        }

    }

    // instead of running jobs synchronously we can also schedule them for asynchronous execution
    val futures = jobs.map { job -> job.schedule(Unit) }

    // retrieve job results
    val results = futures.map { it.get() }
    println(results.toTypedArray().contentToString())

    // when we use the scheduler we need to shut him down with either Scheduler.shutdown() or exitProcess()
    exitProcess(0)
}