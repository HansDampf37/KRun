package org.deg.krun

class Aggregate(
    inputJobs: List<Job<*, *>> = listOf(),
    onReady: (List<*>) -> Unit
) {

    private val previousJobsOutputs = MutableList<Any?>(inputJobs.size) { null }
    private val previousJobsFinished = MutableList(previousJobsOutputs.size) { false }

    init {
        inputJobs.forEachIndexed { index, job ->
            // TODO possible without cast?
            (job as Job<Any?, Any?>).addEventListener(object: JobEventListener<Any?, Any?> {
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