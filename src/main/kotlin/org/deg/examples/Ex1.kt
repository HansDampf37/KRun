package org.deg.examples

import org.deg.krun.Job
import org.deg.krun.JobEventListener

fun main() {
    // Create event listener
    val jobEventListener = object : JobEventListener<String, Int> {
        override fun onStarted(input: String, job: Job<String, Int>) {
            println("Starting to count length of input \"$input\" in job ${job.name}")
        }
        override fun onDone(input: String, output: Int, job: Job<String, Int>) {
            println("Finished counting. The length of \"$input\" is \"$output\"")
        }
    }

    // Create Job and attach event listener
    val countLettersJob = Job(jobEventListener = jobEventListener) { input: String ->
        return@Job input.length
    }

    // execute job
    countLettersJob.run("some string")
}