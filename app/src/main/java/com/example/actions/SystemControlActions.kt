package com.example.actions

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import com.example.engine.Action
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.service.JarvisAccessibilityService

class VolumeAction : Action {
    override val type = ActionType.VOLUME_CONTROL
    override val displayName = "Volume & Sound Control"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionResult.failed("Audio service not available", "Audio service nahi mil saki")

        val mode = (params["mode"] as? String)?.lowercase() ?: ""
        val percentage = (params["percentage"] as? Number)?.toInt()
            ?: (params["percentage"] as? String)?.toIntOrNull()
        val streamType = when ((params["stream"] as? String)?.lowercase()) {
            "ring", "call" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }

        // Silent / Vibrate / Normal modes
        if (mode == "silent" || mode == "mute") {
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                return ActionResult.success(
                    "Phone set to Silent mode",
                    "Phone silent mode par kar diya gaya hai",
                    "Ringer: Silent"
                )
            } catch (e: SecurityException) {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } else Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return ActionResult.permissionRequired(
                    "Notification Policy Access",
                    "Do Not Disturb access is required to silence the phone.",
                    "Phone silent karne ke liye DND permission chahiye.",
                    intent
                )
            }
        } else if (mode == "vibrate") {
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                return ActionResult.success(
                    "Phone set to Vibrate mode",
                    "Phone vibrate mode par set ho gaya hai"
                )
            } catch (e: Exception) {
                // fall through to sound settings
            }
        } else if (mode == "normal") {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }

        val maxVolume = audioManager.getStreamMaxVolume(streamType)

        if (percentage != null) {
            val clamped = percentage.coerceIn(0, 100)
            val targetLevel = (clamped * maxVolume) / 100
            audioManager.setStreamVolume(streamType, targetLevel, AudioManager.FLAG_SHOW_UI)
            return ActionResult.success(
                "Volume adjusted to $clamped%",
                "Volume $clamped percent kar diya gaya hai",
                "Level: $targetLevel / $maxVolume"
            )
        }

        val direction = params["direction"] as? String ?: "up"
        val flag = if (direction == "down" || direction == "kam") AudioManager.ADJUST_LOWER else AudioManager.ADJUST_RAISE
        audioManager.adjustStreamVolume(streamType, flag, AudioManager.FLAG_SHOW_UI)

        return ActionResult.success(
            "Volume adjusted $direction",
            "Volume ${if (direction == "down" || direction == "kam") "kam" else "badha"} diya gaya hai"
        )
    }

    override fun verifyResult(context: Context): Boolean = true
}

class BrightnessAction : Action {
    override val type = ActionType.BRIGHTNESS_CONTROL
    override val displayName = "Screen Brightness Control"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val percentage = (params["percentage"] as? Number)?.toInt()
            ?: (params["percentage"] as? String)?.toIntOrNull()
            ?: 50
        val clamped = percentage.coerceIn(0, 100)
        val brightnessValue = (clamped * 255) / 100

        val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true

        if (canWrite) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessValue
                )
                return ActionResult.success(
                    "Brightness set to $clamped%",
                    "Brightness $clamped percent set ho gaya hai",
                    "Value: $brightnessValue / 255"
                )
            } catch (e: Exception) {
                // fall back to opening settings
            }
        }

        // Android policy requires Write Settings permission for direct change
        val displaySettingsIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActionResult.unsupported(
            "Android requires System Write permission to alter brightness directly. Opening Display Settings.",
            "Direct brightness badalne ke liye system permission chahiye. Display settings open kar raha hoon.",
            displaySettingsIntent
        )
    }

    override fun verifyResult(context: Context): Boolean = true
}

class FlashlightAction : Action {
    override val type = ActionType.FLASHLIGHT_CONTROL
    override val displayName = "Flashlight / Torch"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val enable = when (val state = params["state"]) {
            is Boolean -> state
            is String -> state.lowercase() != "off" && state.lowercase() != "band"
            else -> true
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ActionResult.failed("Camera service unavailable", "Torch service nahi mil saki")

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull() ?: "0"

            cameraManager.setTorchMode(cameraId, enable)
            val stateText = if (enable) "chalu" else "band"
            ActionResult.success(
                "Torch turned ${if (enable) "ON" else "OFF"}",
                "Torch $stateText kar diya gaya hai",
                "State: ${if (enable) "ON" else "OFF"}"
            )
        } catch (e: Exception) {
            ActionResult.failed("Torch toggle failed: ${e.message}", "Torch on/off nahi ho paya")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class BluetoothAction : Action {
    override val type = ActionType.BLUETOOTH_CONTROL
    override val displayName = "Bluetooth Control"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.success(
                "Bluetooth settings opened",
                "Bluetooth settings khol diya gaya hai",
                "Action: ACTION_BLUETOOTH_SETTINGS"
            )
        } catch (e: Exception) {
            ActionResult.failed("Failed to open Bluetooth settings", "Bluetooth settings nahi khul paya")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class WiFiAction : Action {
    override val type = ActionType.WIFI_CONTROL
    override val displayName = "Wi-Fi Control"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return try {
            context.startActivity(intent)
            ActionResult.success(
                "Wi-Fi panel opened",
                "Wi-Fi control panel khol diya gaya hai",
                "Action: Wi-Fi Panel"
            )
        } catch (e: Exception) {
            ActionResult.failed("Failed to open Wi-Fi panel", "Wi-Fi panel nahi khul paya")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class ScreenshotAction : Action {
    override val type = ActionType.SCREENSHOT
    override val displayName = "Capture Screenshot"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        if (JarvisAccessibilityService.isRunning()) {
            val taken = JarvisAccessibilityService.takeScreenshot()
            return if (taken) {
                ActionResult.success("Screenshot captured", "Screenshot le liya gaya hai")
            } else {
                ActionResult.failed("Failed to capture screenshot", "Screenshot nahi liya ja saka")
            }
        }

        val accessIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActionResult.unsupported(
            "Accessibility Service is required for automated screenshot capture.",
            "Hands-free screenshot lene ke liye Jarvis Accessibility Service chalu honi chahiye.",
            accessIntent
        )
    }

    override fun verifyResult(context: Context): Boolean = true
}

class CameraAction : Action {
    override val type = ActionType.CAMERA_LAUNCH
    override val displayName = "Launch Camera"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.success("Camera opened", "Camera khol diya gaya hai")
        } catch (e: Exception) {
            val fallback = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
                ActionResult.success("Camera opened", "Camera khol diya gaya hai")
            } catch (ex: Exception) {
                ActionResult.failed("Failed to launch camera", "Camera nahi khul saka")
            }
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class OpenSettingsAction : Action {
    override val type = ActionType.OPEN_SETTINGS
    override val displayName = "System Settings"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val page = (params["page"] as? String)?.lowercase() ?: ""
        val intent = when {
            page.contains("sound") || page.contains("volume") || page.contains("awaz") ->
                Intent(Settings.ACTION_SOUND_SETTINGS)
            page.contains("display") || page.contains("brightness") || page.contains("screen") ->
                Intent(Settings.ACTION_DISPLAY_SETTINGS)
            page.contains("battery") || page.contains("charging") ->
                Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            page.contains("location") || page.contains("gps") ->
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            page.contains("airplane") || page.contains("flight") ->
                Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            page.contains("app") || page.contains("apps") ->
                Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            page.contains("access") ->
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            page.contains("notif") ->
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            else ->
                Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ActionResult.success("Settings opened: $page", "Settings khol diya gaya hai")
        } catch (e: Exception) {
            ActionResult.failed("Failed to open Settings", "Settings nahi khul saka")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}
