package com.akingstar.askai

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class AskAINotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = sbn?.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val body = extras.getCharSequence("android.text")?.toString().orEmpty()
        val safe = SensitiveDataFilter.mask("$title: $body")
        getSharedPreferences("askai_history", MODE_PRIVATE).edit()
            .putString("last_notification", safe.take(1000)).apply()
    }
}
