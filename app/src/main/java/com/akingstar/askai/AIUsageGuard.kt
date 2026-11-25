package com.akingstar.askai

import android.content.Context
import java.security.MessageDigest
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

internal object AIUsagePolicy {
    const val DAILY_LIMIT = 20
    const val DUPLICATE_WINDOW_MS = 5_000L

    fun canReserve(used: Int, limit: Int = DAILY_LIMIT): Boolean =
        used >= 0 && limit > 0 && used < limit

    fun countForDay(storedDay: String?, currentDay: String, storedCount: Int): Int =
        if (storedDay == currentDay) storedCount.coerceAtLeast(0) else 0

    fun isDuplicate(
        currentFingerprint: String,
        previousFingerprint: String,
        nowMillis: Long,
        previousMillis: Long,
        windowMillis: Long = DUPLICATE_WINDOW_MS
    ): Boolean {
        if (currentFingerprint.isBlank() ||
            currentFingerprint != previousFingerprint ||
            windowMillis <= 0 ||
            nowMillis < previousMillis
        ) return false

        return nowMillis - previousMillis < windowMillis
    }
}

internal data class AIUsageSnapshot(
    val dayKey: String,
    val used: Int,
    val limit: Int
) {
    val remaining: Int
        get() = (limit - used).coerceAtLeast(0)
}

internal object AIUsageGuard {
    private const val DAY_KEY = "ai_usage_day"
    private const val COUNT_KEY = "ai_usage_attempts"
    private const val LAST_HASH_KEY = "ai_usage_last_hash"
    private const val LAST_TIME_KEY = "ai_usage_last_time"

    private val lock = Any()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("askai", Context.MODE_PRIVATE)

    private fun today(now: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

    // If persistence fails, callers fail closed.
    private fun resetIfNewDay(
        preferences: android.content.SharedPreferences,
        day: String
    ): Boolean {
        val oldDay = preferences.getString(DAY_KEY, null)
        if (oldDay == day) return true

        return preferences.edit()
            .putString(DAY_KEY, day)
            .putInt(COUNT_KEY, 0)
            .remove(LAST_HASH_KEY)
            .remove(LAST_TIME_KEY)
            .commit()
    }

    private fun fingerprint(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    // Call only when at least one online provider is configured.
    // No raw command text is persisted.
    fun beginRequest(context: Context, text: String): Boolean = synchronized(lock) {
        val preferences = prefs(context)
        val now = System.currentTimeMillis()

        if (!resetIfNewDay(preferences, today(now))) {
            return@synchronized false
        }

        val hash = fingerprint(text)
        val oldHash = preferences.getString(LAST_HASH_KEY, "").orEmpty()
        val oldTime = preferences.getLong(LAST_TIME_KEY, 0L)

        if (AIUsagePolicy.isDuplicate(hash, oldHash, now, oldTime)) {
            return@synchronized false
        }

        preferences.edit()
            .putString(LAST_HASH_KEY, hash)
            .putLong(LAST_TIME_KEY, now)
            .commit()
    }

    // Reserve immediately before each outbound HTTP call, including retries.
    fun reserveOnlineAttempt(context: Context): Boolean = synchronized(lock) {
        val preferences = prefs(context)
        if (!resetIfNewDay(preferences, today())) {
            return@synchronized false
        }

        val used = preferences.getInt(COUNT_KEY, 0).coerceAtLeast(0)
        if (!AIUsagePolicy.canReserve(used)) {
            return@synchronized false
        }

        preferences.edit().putInt(COUNT_KEY, used + 1).commit()
    }

    fun snapshot(context: Context): AIUsageSnapshot = synchronized(lock) {
        val preferences = prefs(context)
        val day = today()
        val resetSucceeded = resetIfNewDay(preferences, day)
        val used = if (resetSucceeded) {
            AIUsagePolicy.countForDay(
                preferences.getString(DAY_KEY, null),
                day,
                preferences.getInt(COUNT_KEY, 0)
            )
        } else {
            AIUsagePolicy.DAILY_LIMIT
        }

        AIUsageSnapshot(day, used, AIUsagePolicy.DAILY_LIMIT)
    }
}
