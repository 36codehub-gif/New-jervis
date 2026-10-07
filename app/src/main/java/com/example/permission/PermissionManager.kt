package com.example.permission

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationListenerService

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isSpecial: Boolean = false,
    val settingsIntent: Intent
)

class PermissionManager(private val context: Context) {

    fun getPermissionStatusList(): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = Manifest.permission.RECORD_AUDIO,
                title = "Microphone Access",
                description = "Required to capture and transcribe voice commands in Hindi and English.",
                isGranted = micGranted,
                settingsIntent = createAppSettingsIntent()
            )
        )

        // 2. Notification Listener
        val notifListenerGranted = JarvisNotificationListenerService.isNotificationAccessGranted(context)
        list.add(
            PermissionItem(
                id = "special.notification_listener",
                title = "Notification Listener Access",
                description = "Allows Jarvis to read notifications aloud and announce important alerts upon request.",
                isGranted = notifListenerGranted,
                isSpecial = true,
                settingsIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        )

        // 3. Accessibility Service
        val accessGranted = JarvisAccessibilityService.isAccessibilityEnabled(context)
        list.add(
            PermissionItem(
                id = "special.accessibility",
                title = "Accessibility Automation Service",
                description = "Enables hands-free navigation gestures (Back, Home, Recents, Screenshot) and screen automation.",
                isGranted = accessGranted,
                isSpecial = true,
                settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        )

        // 4. Phone Calling
        val callGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = Manifest.permission.CALL_PHONE,
                title = "Phone Call Permission",
                description = "Enables calling contacts directly when commanded (e.g., 'Papa ko call karo').",
                isGranted = callGranted,
                settingsIntent = createAppSettingsIntent()
            )
        )

        // 5. Contacts
        val contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = Manifest.permission.READ_CONTACTS,
                title = "Contacts Search Access",
                description = "Allows searching names and phone numbers from your address book.",
                isGranted = contactsGranted,
                settingsIntent = createAppSettingsIntent()
            )
        )

        // 6. SMS
        val smsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = Manifest.permission.SEND_SMS,
                title = "Send SMS Permission",
                description = "Used to compose and send quick text messages upon verbal command.",
                isGranted = smsGranted,
                settingsIntent = createAppSettingsIntent()
            )
        )

        // 7. Camera / Torch
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = Manifest.permission.CAMERA,
                title = "Camera & Torch Access",
                description = "Required to toggle device flashlight and quickly launch the camera.",
                isGranted = cameraGranted,
                settingsIntent = createAppSettingsIntent()
            )
        )

        // 8. Notification Policy / DND
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val dndGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notifManager?.isNotificationPolicyAccessGranted == true
        } else true
        list.add(
            PermissionItem(
                id = "special.dnd",
                title = "Do Not Disturb / Sound Policy",
                description = "Needed to automatically mute, silence or unmute your device in routines like Study Mode.",
                isGranted = dndGranted,
                isSpecial = true,
                settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } else createAppSettingsIntent()
            )
        )

        // 9. Post Notifications
        val postNotifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        list.add(
            PermissionItem(
                id = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else "android.permission.POST_NOTIFICATIONS",
                title = "Post Notifications",
                description = "Displays background listening status and completed task notifications.",
                isGranted = postNotifGranted,
                settingsIntent = createAppSettingsIntent()
            )
        )

        // 10. Write Settings (for direct brightness modification if permitted)
        val canWriteSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true
        list.add(
            PermissionItem(
                id = "special.write_settings",
                title = "Modify System Settings",
                description = "Allows Jarvis to directly adjust screen brightness without redirecting to display settings.",
                isGranted = canWriteSettings,
                isSpecial = true,
                settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else createAppSettingsIntent()
            )
        )

        return list
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
