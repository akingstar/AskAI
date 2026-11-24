package com.akingstar.askai.automation

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Locale
import java.util.concurrent.TimeUnit

internal object RoutineSchedulePolicy {
    fun isValidName(name: String): Boolean {
        val clean = name.trim()
        return clean.isNotBlank() && clean.length <= 40
    }

    fun isValidDelay(minutes: Long): Boolean = minutes in 1..525600

    fun isValidRecurringInterval(minutes: Long): Boolean =
        minutes in 15..525600
}

internal object RoutineWorkNames {
    private fun normalized(name: String): String =
        name.trim().lowercase(Locale.ROOT)

    fun oneTime(name: String): String =
        "askai-reminder-once-${normalized(name)}"

    fun recurring(name: String): String =
        "askai-reminder-recurring-${normalized(name)}"

    // Previous app versions used one shared unique-work name.
    fun legacy(name: String): String =
        "askai-reminder-${name.trim().lowercase()}"
}

class RoutineScheduler(context: Context) {
    private val app = context.applicationContext
    private val work = WorkManager.getInstance(app)

    fun schedule(name: String, delayMinutes: Long): Boolean {
        val clean = name.trim()
        if (!RoutineSchedulePolicy.isValidName(clean) ||
            !RoutineSchedulePolicy.isValidDelay(delayMinutes)
        ) return false

        return runCatching {
            val data = Data.Builder()
                .putString("routine_name", clean)
                .build()

            val request = OneTimeWorkRequestBuilder<RoutineReminderWorker>()
                .setInputData(data)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            work.cancelUniqueWork(RoutineWorkNames.legacy(clean))
            work.enqueueUniqueWork(
                RoutineWorkNames.oneTime(clean),
                ExistingWorkPolicy.REPLACE,
                request
            )
            true
        }.getOrDefault(false)
    }

    fun scheduleRecurring(name: String, intervalMinutes: Long): Boolean {
        val clean = name.trim()
        if (!RoutineSchedulePolicy.isValidName(clean) ||
            !RoutineSchedulePolicy.isValidRecurringInterval(intervalMinutes)
        ) return false

        return runCatching {
            val data = Data.Builder()
                .putString("routine_name", clean)
                .build()

            val request = PeriodicWorkRequestBuilder<RoutineReminderWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            )
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            work.cancelUniqueWork(RoutineWorkNames.legacy(clean))
            work.enqueueUniquePeriodicWork(
                RoutineWorkNames.recurring(clean),
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
            true
        }.getOrDefault(false)
    }

    fun cancel(name: String): Boolean {
        val clean = name.trim()
        if (!RoutineSchedulePolicy.isValidName(clean)) return false

        return runCatching {
            work.cancelUniqueWork(RoutineWorkNames.legacy(clean))
            work.cancelUniqueWork(RoutineWorkNames.oneTime(clean))
            work.cancelUniqueWork(RoutineWorkNames.recurring(clean))
            true
        }.getOrDefault(false)
    }
}
