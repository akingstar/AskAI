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

    fun listScheduled(
        routines: List<SavedRoutine>,
        onResult: (String) -> Unit
    ) {
        Thread({
            val lines = mutableListOf<String>()
            var totalActive = 0
            var timedOut = false
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6)

            for (routine in routines.take(50)) {
                val candidates = listOf(
                    "एक बार" to RoutineWorkNames.oneTime(routine.name),
                    "दोहराने वाला" to RoutineWorkNames.recurring(routine.name),
                    "पुराना" to RoutineWorkNames.legacy(routine.name)
                )

                for ((label, uniqueName) in candidates) {
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0L) {
                        timedOut = true
                        break
                    }

                    val infos = runCatching {
                        work.getWorkInfosForUniqueWork(uniqueName)
                            .get(remaining, TimeUnit.NANOSECONDS)
                    }.getOrElse {
                        if (System.nanoTime() >= deadline) timedOut = true
                        emptyList()
                    }

                    for (info in infos) {
                        val state = when (info.state) {
                            androidx.work.WorkInfo.State.ENQUEUED -> "आने वाला"
                            androidx.work.WorkInfo.State.RUNNING -> "चल रहा है"
                            androidx.work.WorkInfo.State.BLOCKED -> "रुका हुआ"
                            else -> null
                        } ?: continue

                        totalActive++
                        if (lines.size < 12) {
                            lines.add("${routine.name} — $label — $state")
                        }
                    }
                    if (timedOut) break
                }
                if (timedOut) break
            }

            val result = buildString {
                if (lines.isEmpty()) {
                    append("अभी कोई pending routine reminder नहीं मिला।")
                } else {
                    append("Scheduled reminders:\n")
                    append(lines.joinToString("\n"))
                    if (totalActive > lines.size) {
                        append("\nऔर ${totalActive - lines.size} reminders भी हैं।")
                    }
                }
                if (timedOut) {
                    append("\nकुछ reminders की जाँच समय सीमा के कारण पूरी नहीं हो सकी।")
                }
            }
            onResult(result)
        }, "AskAI-Reminder-Status").apply {
            isDaemon = true
            start()
        }
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
