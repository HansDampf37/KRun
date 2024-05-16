package org.deg.krun

interface JobEventListeners<I, O> {
    fun onStarted(input: I, job: Job<I, O>) {}
    fun onDone(output: O, job: Job<I, O>) {}
    fun onFailure(exception: Exception, job: Job<I, O>) {}
}
