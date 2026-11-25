package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test
import android.provider.Settings

class BrightnessPolicyTest {
    @Test fun parsesOnlyIntegerPercentagesFromOneToOneHundred() {
        assertEquals(1, BrightnessPolicy.parsePercent("1"))
        assertEquals(50, BrightnessPolicy.parsePercent("50"))
        assertEquals(100, BrightnessPolicy.parsePercent("100"))
        listOf("0", "101", "-1", "1.5", "", "up").forEach {
            assertNull(BrightnessPolicy.parsePercent(it))
        }
    }

    @Test fun convertsPercentToAndroidBrightnessRange() {
        assertEquals(3, BrightnessPolicy.toSystemValue(1))
        assertEquals(128, BrightnessPolicy.toSystemValue(50))
        assertEquals(255, BrightnessPolicy.toSystemValue(100))
        assertNull(BrightnessPolicy.toSystemValue(0))
        assertNull(BrightnessPolicy.toSystemValue(101))
    }

    @Test fun convertsSystemBrightnessBackToPercent() {
        assertEquals(1, BrightnessPolicy.fromSystemValue(0))
        assertEquals(50, BrightnessPolicy.fromSystemValue(128))
        assertEquals(100, BrightnessPolicy.fromSystemValue(255))
        assertNull(BrightnessPolicy.fromSystemValue(-1))
        assertNull(BrightnessPolicy.fromSystemValue(256))
    }

    @Test fun adjustmentIsBoundedToOneThroughOneHundred() {
        assertEquals(60, BrightnessPolicy.adjust(50, "up"))
        assertEquals(40, BrightnessPolicy.adjust(50, "down"))
        assertEquals(100, BrightnessPolicy.adjust(95, "up"))
        assertEquals(1, BrightnessPolicy.adjust(5, "down"))
        assertNull(BrightnessPolicy.adjust(50, "other"))
    }

    @Test fun onlyManualModeIsAllowed() {
        assertTrue(BrightnessPolicy.isManualMode(
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        ))
        assertFalse(BrightnessPolicy.isManualMode(
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        ))
        assertFalse(BrightnessPolicy.isManualMode(-1))
    }
}
