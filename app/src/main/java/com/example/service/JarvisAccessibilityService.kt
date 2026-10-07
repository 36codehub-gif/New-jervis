package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track active window or events if needed
    }

    override fun onInterrupt() {
        // Interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        fun takeScreenshot(): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                false
            }
        }

        fun performHome(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        fun performBack(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        fun performRecents(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
        fun performNotifications(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        fun performQuickSettings(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) ?: false

        fun clickText(targetText: String): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(targetText)
            for (node in nodes) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                }
            }
            return false
        }

        fun typeText(text: String): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        fun readVisibleScreenText(): String {
            val root = instance?.rootInActiveWindow ?: return "Screen content unavailable"
            val sb = StringBuilder()
            extractNodeText(root, sb)
            return if (sb.isEmpty()) "No visible text detected on screen" else sb.toString()
        }

        private fun extractNodeText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
            if (node == null) return
            node.text?.let {
                if (it.isNotBlank()) sb.append(it).append(" ")
            }
            node.contentDescription?.let {
                if (it.isNotBlank()) sb.append(it).append(" ")
            }
            for (i in 0 until node.childCount) {
                extractNodeText(node.getChild(i), sb)
            }
        }
    }
}
