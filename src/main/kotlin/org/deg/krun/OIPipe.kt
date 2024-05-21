package org.deg.krun

open class OIPipe<O, I>(fromJob: Job<*, O>, val toJob: Job<I, *>, conversion: (output: O) -> I) {
    init {
        val jobEventListener = object : IJobEventListener<Any?, O> {
            override fun onDone(input: Any?, output: O, job: Job<Any?, O>) {
                val convertedInput = conversion(output)
                toJob.run(convertedInput)
            }
        }
        // TODO possible without cast?
        (fromJob as Job<Any?, O>).addEventListener(jobEventListener)
    }
}

class Pipe<T>(fromJob: Job<*, T>, toJob: Job<T, *>): OIPipe<T, T>(fromJob, toJob, this::identity) {
    companion object {
        fun <T> identity(lastJobsOutput: T): T {
            return lastJobsOutput
        }
    }
}