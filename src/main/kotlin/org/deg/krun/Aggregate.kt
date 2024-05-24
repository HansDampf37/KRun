package org.deg.krun

import java.io.File

class Aggregate(
    inputJobs: List<Job<*, *>> = listOf(),
    onReady: (List<*>) -> Unit
) {

    private val previousJobsOutputs = MutableList<Any?>(inputJobs.size) { null }
    private val previousJobsFinished = MutableList(previousJobsOutputs.size) { false }

    init {
        inputJobs.forEachIndexed { index, job ->
            // TODO possible without cast?
            (job as Job<Any?, Any?>).addEventListener(object: IJobEventListener<Any?, Any?> {
                override fun onDone(input: Any?, output: Any?, job: Job<Any?, Any?>) {
                    previousJobsOutputs[index] = output
                    previousJobsFinished[index] = true
                    if (isReady()) onReady(previousJobsOutputs)
                }
            })
        }
    }

    fun isReady(): Boolean {
        return previousJobsFinished.all { it }
    }
}

fun main() {
    val scheduler = Scheduler()
    val downloadJob = Job<String, File> {
        File(it)
    }
    val countLines = Job<String, Int> {
        it.lines().size
    }
    val futureForProcessingDownload = scheduler.scheduleAfter(countLines, downloadJob) {
        it.readText()
    }
    scheduler.schedule(downloadJob, "README.md")
    val amountOfLines = futureForProcessingDownload.get()
    println("Readme has $amountOfLines lines")
    scheduler.shutdown()
}