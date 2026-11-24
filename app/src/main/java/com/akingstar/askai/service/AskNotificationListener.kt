package com.akingstar.askai.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class AskNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) { /* No notification content is uploaded or stored. */ }
}
