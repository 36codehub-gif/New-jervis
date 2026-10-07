package com.example.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.engine.Action
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.service.JarvisAccessibilityService
import com.example.voice.InstalledAppManager

class OpenAppAction(private val appManager: InstalledAppManager) : Action {
    override val type = ActionType.OPEN_APP
    override val displayName = "Open Application"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val appName = params["target"] as? String ?: params["appName"] as? String
        if (appName.isNullOrBlank()) {
            return ActionResult.failed(
                "App name not specified",
                "Kaunsa application kholna hai? Kripya naam batayein."
            )
        }

        val app = appManager.findApp(appName)
        if (app != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult.success(
                    "${app.label} launched successfully",
                    "${app.label} khola ja raha hai",
                    "Package: ${app.packageName}"
                )
            }
        }

        // Try direct browser or market search if app not found
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActionResult.failed(
            "Application '$appName' not found on device",
            "Mujhe '$appName' app phone mein nahi mila.",
            "Aap Play Store par dhund sakte hain"
        )
    }

    override fun verifyResult(context: Context): Boolean = true
}

class CloseAppAction : Action {
    override val type = ActionType.ACCESSIBILITY_GESTURE
    override val displayName = "Close Current Application"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        if (JarvisAccessibilityService.isRunning()) {
            val success = JarvisAccessibilityService.performHome()
            return if (success) {
                ActionResult.success("Navigated to Home screen", "Home screen par wapas ja rahe hain")
            } else {
                ActionResult.failed("Failed to send Home gesture", "Home screen par nahi ja paye")
            }
        }

        // Without accessibility, Android does not permit killing third-party apps directly
        return ActionResult.unsupported(
            "Android does not allow background app killing without Accessibility",
            "Android direct app band karne ki anumati nahi deta. Main Accessibility service ke zariye Home par le ja sakta hoon."
        )
    }

    override fun verifyResult(context: Context): Boolean = true
}

class LaunchActivityAction : Action {
    override val type = ActionType.OPEN_APP
    override val displayName = "Launch Activity"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val packageName = params["packageName"] as? String ?: return ActionResult.failed("Missing package name", "Package name missing hai")
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launchIntent != null) {
            context.startActivity(launchIntent)
            ActionResult.success("Activity launched", "App khol diya gaya hai")
        } else {
            ActionResult.failed("Failed to find launcher activity", "App launch nahi ho paya")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class AppSearchAction(private val appManager: InstalledAppManager) : Action {
    override val type = ActionType.OPEN_APP
    override val displayName = "Search Installed Apps"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val query = params["query"] as? String ?: ""
        val apps = if (query.isBlank()) appManager.getAllApps() else appManager.getAllApps().filter {
            it.label.contains(query, ignoreCase = true) || it.aliases.any { alias -> alias.contains(query, ignoreCase = true) }
        }
        val summary = apps.take(5).joinToString(", ") { it.label }
        return ActionResult.success(
            "Found ${apps.size} matching apps",
            if (apps.isEmpty()) "Koi app nahi mila" else "Mile hue apps: $summary",
            apps.joinToString("\n") { "${it.label} (${it.packageName})" }
        )
    }

    override fun verifyResult(context: Context): Boolean = true
}
