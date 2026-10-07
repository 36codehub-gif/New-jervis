package com.example.actions

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.view.KeyEvent
import com.example.engine.Action
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationListenerService

class WebSearchAction : Action {
    override val type = ActionType.WEB_SEARCH
    override val displayName = "Web & Online Search"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val query = params["query"] as? String ?: params["target"] as? String ?: ""
        val platform = (params["platform"] as? String)?.lowercase() ?: ""

        if (query.isBlank()) {
            return ActionResult.failed("Search query missing", "Kya search karna hai? Batayein.")
        }

        if (platform.contains("youtube") || query.contains("youtube", ignoreCase = true)) {
            val cleanQuery = query.replace("youtube", "", ignoreCase = true).trim()
            val ytIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", cleanQuery)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(ytIntent)
                ActionResult.success(
                    "Searching YouTube for '$cleanQuery'",
                    "YouTube par '$cleanQuery' search kiya ja raha hai",
                    "Query: $cleanQuery"
                )
            } catch (e: Exception) {
                // Fallback web url
                val webYt = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(cleanQuery)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webYt)
                ActionResult.success("YouTube web search opened", "YouTube par search khol diya hai")
            }
        }

        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(searchIntent)
            ActionResult.success(
                "Searching web for '$query'",
                "Internet par '$query' search kiya ja raha hai",
                "Query: $query"
            )
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult.success("Search opened in browser", "Browser par search open kar diya hai")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class MapsNavigationAction : Action {
    override val type = ActionType.MAPS_NAVIGATION
    override val displayName = "Maps & Navigation"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val destination = params["destination"] as? String ?: params["target"] as? String ?: ""
        if (destination.isBlank()) {
            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(mapsIntent)
                ActionResult.success("Google Maps opened", "Google Maps open kar diya hai")
            } catch (e: Exception) {
                ActionResult.failed("Failed to open Maps", "Google Maps nahi khul saka")
            }
        }

        val navUri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(mapIntent)
            ActionResult.success(
                "Navigation started to '$destination'",
                "'$destination' ke liye navigation shuru kiya ja raha hai",
                "Destination: $destination"
            )
        } catch (e: Exception) {
            // General geo fallback
            val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(destination)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(geoIntent)
                ActionResult.success("Maps route opened for '$destination'", "'$destination' ka route Maps par khol diya hai")
            } catch (ex: Exception) {
                ActionResult.failed("Maps navigation unavailable", "Navigation shuru nahi ho paya")
            }
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class MediaControlAction : Action {
    override val type = ActionType.MEDIA_CONTROL
    override val displayName = "Media Playback Control"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val command = (params["command"] as? String)?.lowercase() ?: "play_pause"
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionResult.failed("Audio service unavailable", "Media service nahi mil saki")

        val keyCode = when (command) {
            "play", "chalao", "shuru" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause", "roko", "band" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next", "agla", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "pichla" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)

        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)

        val speech = when (command) {
            "play", "chalao" -> "Music play kiya ja raha hai"
            "pause", "roko" -> "Music pause kar diya gaya hai"
            "next", "agla" -> "Next song par switch kiya ja raha hai"
            "previous", "pichla" -> "Previous song par ja rahe hain"
            else -> "Media playback command execute ho gaya hai"
        }

        return ActionResult.success("Media command: $command executed", speech, "KeyCode: $keyCode")
    }

    override fun verifyResult(context: Context): Boolean = true
}

class NotificationAction : Action {
    override val type = ActionType.NOTIFICATION_READ
    override val displayName = "Notification Management"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val subAction = params["subAction"] as? String ?: "read"

        if (!JarvisNotificationListenerService.isNotificationAccessGranted(context)) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return ActionResult.permissionRequired(
                "Notification Listener Access",
                "Notification Listener permission is needed to read recent notifications.",
                "Notification padhne ke liye Jarvis ko notification access dena hoga.",
                intent
            )
        }

        if (subAction == "dismiss" || subAction == "clear") {
            val dismissed = JarvisNotificationListenerService.dismissLatest()
            return if (dismissed) {
                ActionResult.success("Notification dismissed", "Notification hata diya gaya hai")
            } else {
                ActionResult.failed("No active notification to dismiss", "Koi notification hatane ke liye nahi hai")
            }
        }

        val summary = JarvisNotificationListenerService.getLatestNotificationSummary()
        val all = JarvisNotificationListenerService.getRecentNotificationsList()
        val details = if (all.isEmpty()) "No notifications found" else all.take(5).joinToString("\n\n") {
            "• [${it.appName}] ${it.title}: ${it.text}"
        }

        return ActionResult.success(
            "Recent notifications checked",
            summary,
            details,
            payload = all
        )
    }

    override fun verifyResult(context: Context): Boolean = true
}

class FileSearchAction : Action {
    override val type = ActionType.FILE_SEARCH
    override val displayName = "Search Files & Photos"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val query = params["query"] as? String ?: params["target"] as? String ?: ""
        val category = (params["category"] as? String)?.lowercase() ?: ""

        val intent = when {
            category.contains("photo") || category.contains("tasveer") || query.contains("photo", ignoreCase = true) || query.contains("screenshot", ignoreCase = true) -> {
                Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            category.contains("pdf") || query.contains("pdf", ignoreCase = true) -> {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/pdf"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            else -> {
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }

        return try {
            context.startActivity(intent)
            ActionResult.success(
                "File explorer opened for '$query'",
                "Files explorer mein '${if (query.isNotBlank()) query else "files"}' khola ja raha hai",
                "Search category: $category"
            )
        } catch (e: Exception) {
            ActionResult.failed("Failed to open file viewer", "File viewer nahi khul saka")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class ClipboardAction : Action {
    override val type = ActionType.CLIPBOARD_ACTION
    override val displayName = "Clipboard Operations"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return ActionResult.failed("Clipboard service unavailable", "Clipboard nahi mil saka")

        val textToCopy = params["text"] as? String
        return if (!textToCopy.isNullOrBlank()) {
            val clip = ClipData.newPlainText("Jarvis", textToCopy)
            clipboard.setPrimaryClip(clip)
            ActionResult.success("Text copied to clipboard", "Text clipboard par copy ho gaya hai", textToCopy)
        } else {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                ActionResult.success("Clipboard content retrieved", "Clipboard par likha hai: $text", text)
            } else {
                ActionResult.failed("Clipboard is empty", "Clipboard khali hai")
            }
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class AccessibilityAction : Action {
    override val type = ActionType.ACCESSIBILITY_GESTURE
    override val displayName = "Accessibility Automation Gesture"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        if (!JarvisAccessibilityService.isRunning()) {
            val intent = Intent("android.settings.ACCESSIBILITY_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return ActionResult.permissionRequired(
                "Accessibility Service",
                "Jarvis Accessibility Service must be enabled in Settings.",
                "Screen gestures ke liye Accessibility Service chalu karein.",
                intent
            )
        }

        val gesture = (params["gesture"] as? String)?.lowercase() ?: "back"
        val success = when (gesture) {
            "back", "piche" -> JarvisAccessibilityService.performBack()
            "home", "mukhya" -> JarvisAccessibilityService.performHome()
            "recents", "recent_apps" -> JarvisAccessibilityService.performRecents()
            "notifications", "shade" -> JarvisAccessibilityService.performNotifications()
            "quick_settings" -> JarvisAccessibilityService.performQuickSettings()
            "click" -> {
                val targetText = params["text"] as? String ?: ""
                JarvisAccessibilityService.clickText(targetText)
            }
            "type" -> {
                val inputText = params["text"] as? String ?: ""
                JarvisAccessibilityService.typeText(inputText)
            }
            else -> false
        }

        return if (success) {
            ActionResult.success("Gesture $gesture executed", "Gesture execute ho gaya hai")
        } else {
            ActionResult.failed("Gesture $gesture could not be completed", "Gesture execute nahi ho paya")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}
