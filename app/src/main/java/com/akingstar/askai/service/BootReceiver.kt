package com.akingstar.askai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent?) { if (intent?.action == Intent.ACTION_BOOT_COMPLETED) { val req = PeriodicWorkRequestBuilder<ReminderWatchWorker>(12, TimeUnit.HOURS).build(); WorkManager.getInstance(context).enqueueUniquePeriodicWork("askai-reminder-watch", ExistingPeriodicWorkPolicy.KEEP, req) } } }
class ReminderWatchWorker(context: Context, params: WorkerParameters) : Worker(context, params) { override fun doWork(): Result = Result.success() }
