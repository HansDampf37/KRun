package org.deg.examples

import org.deg.krun.Aggregate
import org.deg.krun.Job
import org.deg.krun.Scheduler
import org.deg.krun.schedule

fun main() {
    val job1 = Job<Unit, Int> {
        return@Job 3
    }
    val job2 = Job<Unit, String> {
        return@Job "Chinesen mit nem Kontrabass"
    }
    val printJob = Job<Any, Unit> {
        println(it)
    }
    Aggregate(listOf(job1, job2)) {
        printJob.run(it)
    }
    job1.schedule(Unit).get()
    job2.schedule(Unit).get()
    Scheduler.shutdown()
}