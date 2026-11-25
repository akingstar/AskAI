package com.akingstar.askai

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Intentionally does not read, store, or transmit notification content.
 * Notification access is not registered in the app manifest.
 */
class AskAINotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) = Unit
}
