package org.deg.krun

class Utils {
    companion object {
        fun <T, S : T> identity(lastJobsOutput: S): T {
            return lastJobsOutput
        }
    }
}