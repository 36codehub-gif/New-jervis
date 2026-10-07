package com.example.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.example.engine.Action
import com.example.model.ActionResult
import com.example.model.ActionType

class SetAlarmAction : Action {
    override val type = ActionType.SET_ALARM
    override val displayName = "Set Alarm"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = listOf("com.android.alarm.permission.SET_ALARM")

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val hour = (params["hour"] as? Number)?.toInt()
            ?: (params["hour"] as? String)?.toIntOrNull()
            ?: 7
        val minute = (params["minute"] as? Number)?.toInt()
            ?: (params["minute"] as? String)?.toIntOrNull()
            ?: 0
        val message = params["message"] as? String ?: "Jarvis Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            ActionResult.success(
                "Alarm scheduled for $timeStr",
                "$timeStr baje ka alarm laga diya gaya hai",
                "Label: $message"
            )
        } catch (e: Exception) {
            ActionResult.failed("Failed to schedule alarm", "Alarm set nahi ho paya")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class SetTimerAction : Action {
    override val type = ActionType.SET_TIMER
    override val displayName = "Set Timer"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = listOf("com.android.alarm.permission.SET_ALARM")

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val seconds = (params["seconds"] as? Number)?.toInt()
            ?: (params["seconds"] as? String)?.toIntOrNull()
            ?: 300 // 5 minutes default
        val message = params["message"] as? String ?: "Jarvis Timer"

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val minutes = seconds / 60
            val sec = seconds % 60
            val timeText = if (minutes > 0) "$minutes minute ${if (sec > 0) "$sec second" else ""}" else "$sec second"
            ActionResult.success(
                "Timer started for $timeText",
                "$timeText ka timer shuru kar diya gaya hai",
                "Length: ${seconds}s"
            )
        } catch (e: Exception) {
            ActionResult.failed("Failed to start timer", "Timer shuru nahi ho saka")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class CalendarAction : Action {
    override val type = ActionType.CALENDAR_EVENT
    override val displayName = "Calendar & Reminder"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val title = params["title"] as? String ?: params["message"] as? String ?: "Jarvis Reminder"
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 3600000)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ActionResult.success(
                "Calendar event opened for: $title",
                "Calendar mein '$title' reminder add karne ke liye open kar diya hai",
                "Event title: $title"
            )
        } catch (e: Exception) {
            ActionResult.failed("Failed to open Calendar", "Calendar nahi khul saka")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}
