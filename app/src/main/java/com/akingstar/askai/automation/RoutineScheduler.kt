package com.akingstar.askai.automation

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class RoutineScheduler(context: Context) {
    private val app = context.applicationContext
    private val work = WorkManager.getInstance(app)

    fun schedule(name: String, delayMinutes: Long): Boolean {
        val clean = name.trim()
        if (clean.isBlank() || clean.length > 40) return false
        if (delayMinutes !in 1..525600) return false

        return runCatching {
            val data = Data.Builder()
                .putString("routine_name", clean)
                .build()

            val request = OneTimeWorkRequestBuilder<RoutineReminderWorker>()
                .setInputData(data)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            work.enqueueUniqueWork(
                "askai-reminder-${clean.lowercase()}",
                ExistingWorkPolicy.REPLACE,
                request
            )
            true
        }.getOrDefault(false)
    }

    fun scheduleRecurring(name: String, intervalMinutes: Long): Boolean {
        val clean = name.trim()
        if (clean.isBlank() || clean.length > 40) return false
        if (intervalMinutes !in 15..525600) return false

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
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            work.enqueueUniquePeriodicWork(
                "askai-reminder-${clean.lowercase()}",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
            true
        }.getOrDefault(false)
    }

    fun cancel(name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank() || clean.length > 40) return false

        return runCatching {
            work.cancelUniqueWork("askai-reminder-${clean.lowercase()}")
            true
        }.getOrDefault(false)
    }
}
