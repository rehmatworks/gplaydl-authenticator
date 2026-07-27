package com.gplaydl.authenticator.ui

import com.gplaydl.authenticator.data.Visibility
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateTest {

    @Test
    fun visibilityDefaultsToPrivate() {
        assertEquals(Visibility.Public, Visibility.from("public"))
        assertEquals(Visibility.Private, Visibility.from("private"))
        assertEquals(Visibility.Private, Visibility.from(null))
        assertEquals(Visibility.Private, Visibility.from("unexpected"))
    }

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
