package org.deg.krun

import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant

/**
 * A statistic contains timestamps about a [jobs][Job] [state-changes][Job.state]
 */
class Statistic {
    /**
     * Creation timestamp
     */
    val createdAt: Instant = Instant.now()

    /**
     * Timestamp when [Job] was scheduled by the [Scheduler].
     */
    var scheduledAt: Instant? = null
        private set

    /**
     * Timestamp when [Job] was started.
     */
    var startedAt: Instant? = null
        private set

    /**
     * Timestamp when [Job] finished successfully.
     */
    var succeededAt: Instant? = null
        private set

    /**
     * Timestamp when [Job] was canceled.
     */
    var canceledAt: Instant? = null
        private set

    /**
     * Timestamp when [Job] failed.
     */
    var failedAt: Instant? = null
        private set

    /**
     * Timestamp when [Job] finished successfully / failed / or was canceled.
     */
    val finishedAt get() = succeededAt ?: canceledAt ?: failedAt

    /**
     * Duration between [start][startedAt] and [finish][finishedAt].
     */
    val duration: Duration?
        get() {
            return if (finishedAt == null || startedAt == null) null
            else Duration.between(startedAt, finishedAt)
        }

    /**
     * Duration since [creation][createdAt].
     */
    val age: Duration get() = Duration.between(createdAt, Instant.now())

    /**
     * Update the timestamps depending on the new state
     *
     * @param newState the new state in the [Job]
     */
    fun notifyStateChange(newState: State) {
        when (newState) {
            State.Scheduled -> scheduledAt = Instant.now()
            State.Running -> startedAt = Instant.now()
            State.Canceled -> canceledAt = Instant.now()
            State.Failed -> failedAt = Instant.now()
            State.Done -> succeededAt = Instant.now()
            State.Ready -> throw IllegalArgumentException("Unexpected Change to ${State.Ready}")
        }
    }
}