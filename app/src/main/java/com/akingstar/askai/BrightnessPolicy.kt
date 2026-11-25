package com.akingstar.askai

import kotlin.math.roundToInt

object BrightnessPolicy {
    fun parsePercent(value: String): Int? =
        value.trim().toIntOrNull()?.takeIf { it in 1..100 }

    fun toSystemValue(percent: Int): Int? {
        if (percent !in 1..100) return null
        return (percent * 255f / 100f).roundToInt().coerceIn(1, 255)
    }

    fun fromSystemValue(value: Int): Int? {
        if (value !in 0..255) return null
        return (value * 100f / 255f).roundToInt().coerceIn(1, 100)
    }

    fun adjust(currentPercent: Int, direction: String): Int? {
        if (currentPercent !in 1..100) return null
        return when (direction) {
            "up" -> (currentPercent + 10).coerceAtMost(100)
            "down" -> (currentPercent - 10).coerceAtLeast(1)
            else -> null
        }
    }

    fun isManualMode(mode: Int): Boolean =
        mode == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
}
