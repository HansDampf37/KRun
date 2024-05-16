package org.deg.krun

import kotlin.system.exitProcess

fun main() {
    val jobEventListener = object : JobEventListeners<Unit, Int> {
        override fun onStarted(input: Unit, job: Job<Unit, Int>) = println("started ${job.name}")
        override fun onDone(output: Int, job: Job<Unit, Int>) = println("completed ${job.name}")
        override fun onFailure(exception: Exception, job: Job<Unit, Int>) = println("Failed ${job.name}").apply { exception.printStackTrace() }
    }

    val jobs = List(100) { i ->
        Job(jobEventListener = jobEventListener) { _: Unit ->
            println("executing $i")
            return@Job i
        }

    }

    val futures = jobs.map { job ->
        job.schedule(Unit)
    }

    val results = futures.map { it.get() }
    println(results.toTypedArray().contentToString())
    exitProcess(0)
}