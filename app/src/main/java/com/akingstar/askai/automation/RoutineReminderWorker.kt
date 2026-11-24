package com.akingstar.askai.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.akingstar.askai.MainActivity
import com.akingstar.askai.R

class RoutineReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val name = inputData.getString("routine_name").orEmpty()
        if (name.isBlank()) return Result.failure()

        return try {
            val manager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            val channelId = "askai_routine_reminders"

            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "Routine reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }

            val intent = android.content.Intent(
                applicationContext,
                MainActivity::class.java
            ).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pending = PendingIntent.getActivity(
                applicationContext,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(
                applicationContext,
                channelId
            )
                .setSmallIcon(R.drawable.ic_askai)
                .setContentTitle("Ask AI — रूटीन रिमाइंडर")
                .setContentText("$name रूटीन चलाने का समय हो गया है।")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "$name रूटीन का समय हो गया है। Ask AI खोलकर बोलें: रूटीन चलाओ $name"
                    )
                )
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            manager.notify(name.hashCode(), notification)
            AutomationHistoryStore(applicationContext)
                .record(name, "REMINDER_SENT")
            Result.success()
        } catch (_: SecurityException) {
            AutomationHistoryStore(applicationContext)
                .record(name, "NOTIFICATION_PERMISSION_MISSING")
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 2) Result.retry() else {
                AutomationHistoryStore(applicationContext)
                    .record(name, "REMINDER_FAILED")
                Result.failure()
            }
        }
    }
}
