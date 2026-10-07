package com.example.service

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import com.example.model.NotificationItem
import java.util.concurrent.CopyOnWriteArrayList

class JarvisNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { addOrUpdateNotification(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let { n ->
            recentNotifications.removeAll { it.id == n.key }
        }
    }

    private fun addOrUpdateNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val appName = try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        if (title.isBlank() && text.isBlank()) return

        val item = NotificationItem(
            id = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            isClearable = sbn.isClearable
        )

        recentNotifications.removeAll { it.id == sbn.key }
        recentNotifications.add(0, item)
        if (recentNotifications.size > 50) {
            recentNotifications.removeAt(recentNotifications.lastIndex)
        }
    }

    fun refreshNotifications() {
        try {
            val active = activeNotifications ?: return
            recentNotifications.clear()
            for (sbn in active.reversed()) {
                addOrUpdateNotification(sbn)
            }
        } catch (_: Exception) {
            // Ignore
        }
    }

    companion object {
        @Volatile
        var instance: JarvisNotificationListenerService? = null
            private set

        val recentNotifications = CopyOnWriteArrayList<NotificationItem>()

        fun isNotificationAccessGranted(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false

            val names = flat.split(":").toTypedArray()
            for (name in names) {
                if (!TextUtils.isEmpty(name)) {
                    val cn = android.content.ComponentName.unflattenFromString(name)
                    if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
            return false
        }

        fun getLatestNotificationSummary(): String {
            if (recentNotifications.isEmpty()) {
                instance?.refreshNotifications()
            }
            val latest = recentNotifications.firstOrNull() ?: return "Abhi koi naya notification nahi hai."
            return "${latest.appName} par ${if (latest.title.isNotBlank()) latest.title + " se: " else ""}${latest.text}"
        }

        fun getRecentNotificationsList(): List<NotificationItem> {
            if (recentNotifications.isEmpty()) {
                instance?.refreshNotifications()
            }
            return recentNotifications.toList()
        }

        fun dismissLatest(): Boolean {
            val inst = instance ?: return false
            val latest = recentNotifications.firstOrNull() ?: return false
            try {
                inst.cancelNotification(latest.id)
                recentNotifications.remove(latest)
                return true
            } catch (_: Exception) {
                return false
            }
        }
    }
}
