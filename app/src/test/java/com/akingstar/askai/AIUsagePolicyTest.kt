package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIUsagePolicyTest {
    @Test
    fun allowsAttemptsBelowDailyLimit() {
        assertTrue(AIUsagePolicy.canReserve(0))
        assertTrue(AIUsagePolicy.canReserve(19))
    }

    @Test
    fun blocksTwentyFirstAttempt() {
        assertFalse(AIUsagePolicy.canReserve(20))
        assertFalse(AIUsagePolicy.canReserve(21))
        assertFalse(AIUsagePolicy.canReserve(-1))
    }

    @Test
    fun resetsCountWhenLocalDayChanges() {
        assertEquals(0, AIUsagePolicy.countForDay("2026-11-25", "2026-11-26", 20))
        assertEquals(7, AIUsagePolicy.countForDay("2026-11-26", "2026-11-26", 7))
        assertEquals(0, AIUsagePolicy.countForDay(null, "2026-11-26", 9))
    }

    @Test
    fun blocksSameFingerprintWithinFiveSeconds() {
        assertTrue(AIUsagePolicy.isDuplicate("abc", "abc", 10_000L, 6_000L))
        assertTrue(AIUsagePolicy.isDuplicate("abc", "abc", 10_000L, 5_001L))
    }

    @Test
    fun permitsSameFingerprintAtFiveSecondsAndLater() {
        assertFalse(AIUsagePolicy.isDuplicate("abc", "abc", 15_000L, 10_000L))
        assertFalse(AIUsagePolicy.isDuplicate("abc", "abc", 15_001L, 10_000L))
    }

    @Test
    fun permitsDifferentFingerprint() {
        assertFalse(AIUsagePolicy.isDuplicate("new", "old", 10_000L, 9_000L))
    }

    @Test
    fun clockRollbackDoesNotCreateFalseDuplicate() {
        assertFalse(AIUsagePolicy.isDuplicate("same", "same", 9_000L, 10_000L))
    }
}
