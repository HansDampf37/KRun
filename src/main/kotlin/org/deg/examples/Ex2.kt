package org.deg.examples

import org.deg.krun.Job
import org.deg.krun.IJobEventListener
import org.deg.krun.run

fun main() {
    // create job that will fail
    val failJob = Job { _: Unit ->
        throw Exception("Oups, something went wrong!?")
    }

    // attach event listener
    failJob.addEventListener(object : IJobEventListener<Unit, Nothing> {
        override fun onFailure(exception: Exception, job: Job<Unit, Nothing>) {
            println("Job ${job.name} threw an exception of type ${exception::class}")
        }
    })

    // run job
    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
    failJob.run()
}