package com.akingstar.askai.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.akingstar.askai.MainActivity
import com.akingstar.askai.R

class ListenService : Service() {
    override fun onCreate() { super.onCreate(); val channel = "askai_listening"; if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel, "Ask AI listening", NotificationManager.IMPORTANCE_LOW)); val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE); val n = Notification.Builder(this, channel).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Ask AI").setContentText("Listening service is active only after you start it.").setContentIntent(pi).build(); startForeground(7, n) }
    override fun onBind(intent: Intent?): IBinder? = null
}
