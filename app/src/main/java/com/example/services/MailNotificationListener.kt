package com.example.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MailNotificationListener : NotificationListenerService() {
    companion object {
        const val TAG = "MailNotificationListener"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            // Listen to Outlook or general mail apps
            if (packageName.contains("outlook") || packageName.contains("mail")) {
                val extras = it.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

                if (title.isNotEmpty() || text.isNotEmpty()) {
                    Log.d(TAG, "Intercepted Mail - Title: $title, Text Snippet: $text")
                    // Here we will hook into our Local NLP engine
                    // For now, we broadcast it to the ViewModel
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
