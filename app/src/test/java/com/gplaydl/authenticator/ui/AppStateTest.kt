package com.gplaydl.authenticator.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateTest {

    @Test
    fun pairingCountdownHandlesValidExpiredAndInvalidValues() {
        val now = 1_722_470_400_000L // 2024-08-01T00:00:00Z
        assertEquals(
            90L,
            pairingSecondsRemaining("2024-08-01T00:01:30Z", now),
        )
        assertEquals(
            0L,
            pairingSecondsRemaining("2024-07-31T23:59:00Z", now),
        )
        assertEquals(0L, pairingSecondsRemaining("not-a-date", now))
    }
}
