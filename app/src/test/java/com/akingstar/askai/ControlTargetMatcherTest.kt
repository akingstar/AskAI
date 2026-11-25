package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class ControlTargetMatcherTest {
    @Test fun requiresExactLabel() {
        assertTrue(ControlTargetMatcher.matches("Settings", "settings"))
        assertFalse(ControlTargetMatcher.matches("Settings menu", "Settings"))
        assertFalse(ControlTargetMatcher.matches("My Settings", "Settings"))
    }

    @Test fun normalizesWhitespace() {
        assertTrue(ControlTargetMatcher.matches(" WiFi   Settings ", "wifi settings"))
    }

    @Test fun refusesMissingOrDuplicateMatches() {
        assertEquals(0, ControlTargetMatcher.uniqueExactIndex(
            listOf("Settings", "Help"), "settings"
        ))
        assertNull(ControlTargetMatcher.uniqueExactIndex(
            listOf("Settings", "Settings"), "Settings"
        ))
        assertNull(ControlTargetMatcher.uniqueExactIndex(
            listOf("Settings", "Help"), "Bluetooth"
        ))
    }

    @Test fun refusesBlankTarget() {
        assertFalse(ControlTargetMatcher.matches("Settings", " "))
        assertNull(ControlTargetMatcher.uniqueExactIndex(listOf("Settings"), ""))
    }
}
