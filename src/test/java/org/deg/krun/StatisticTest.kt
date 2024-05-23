package org.deg.krun

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
class StatisticTest {
    @Test
    fun testProperties() {
        val stat = Statistic()
        assertNotNull(stat.createdAt)
        assertNull(stat.duration)
        assertFalse(stat.age.isNegative)
        stat.notifyStateChange(State.Scheduled)
        stat.notifyStateChange(State.Running)
        stat.notifyStateChange(State.Done)
        assertNotNull(stat.scheduledAt)
        assertNotNull(stat.startedAt)
        assertNotNull(stat.succeededAt)
        assertNotNull(stat.finishedAt)
        assertNotNull(stat.duration)
        assertFalse(stat.duration!!.isNegative)
        assertNull(stat.canceledAt)
        assertNull(stat.failedAt)
    }
}