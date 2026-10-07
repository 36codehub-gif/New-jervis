package com.example.engine

import com.example.model.ActionPlan
import com.example.model.ActionType
import java.util.Locale
import java.util.regex.Pattern

class IntentParser {

    fun parse(rawText: String): ActionPlan {
        val trimmed = rawText.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Check for sensitive/dangerous actions requiring explicit confirmation
        if (isSensitiveAction(lower)) {
            val amountMatch = Regex("""(?:rs\.?|inr|rupees?|₹)\s*(\d+)""").find(lower)
                ?: Regex("""(\d+)\s*(?:rs|rupe|rupi|bhej)""").find(lower)
            val recipientMatch = Regex("""([a-zA-Z\u0900-\u097F]+)\s*ko\s*(?:\d+|paise|rupaye)""").find(lower)
            val amount = amountMatch?.groupValues?.get(1) ?: "amount"
            val person = recipientMatch?.groupValues?.get(1)?.replaceFirstChar { it.uppercase() } ?: "Recipient"

            return ActionPlan(
                intent = ActionType.UNKNOWN,
                rawQuery = trimmed,
                confidence = 0.95f,
                requiresConfirmation = true,
                confirmationMessage = "$person ko ₹$amount bhejne ke liye security confirmation chahiye. Kya aap aage badhna chahte hain?",
                parameters = mapOf("person" to person, "amount" to amount, "action" to "payment")
            )
        }

        // 2. Compound: "YouTube kholo aur ... search karo" or "... search karo"
        if (lower.contains("youtube") && (lower.contains("search") || lower.contains("dhundo") || lower.contains("khojo"))) {
            val query = extractSearchQuery(lower, "youtube")
            return ActionPlan(
                intent = ActionType.WEB_SEARCH,
                rawQuery = trimmed,
                parameters = mapOf("query" to query, "platform" to "youtube")
            )
        }

        // 3. Routine trigger check ("study mode", "morning routine", etc.)
        when {
            lower.contains("study mode") || lower.contains("padhai mode") -> {
                return ActionPlan(
                    intent = ActionType.ROUTINE_EXECUTE,
                    rawQuery = trimmed,
                    parameters = mapOf("routine" to "study mode")
                )
            }
            lower.contains("morning routine") || lower.contains("subah") && lower.contains("routine") -> {
                return ActionPlan(
                    intent = ActionType.ROUTINE_EXECUTE,
                    rawQuery = trimmed,
                    parameters = mapOf("routine" to "morning routine")
                )
            }
            lower.contains("workout mode") || lower.contains("gym mode") -> {
                return ActionPlan(
                    intent = ActionType.ROUTINE_EXECUTE,
                    rawQuery = trimmed,
                    parameters = mapOf("routine" to "workout mode")
                )
            }
            lower.contains("bedtime") || lower.contains("sone ka time") || lower.contains("good night") -> {
                return ActionPlan(
                    intent = ActionType.ROUTINE_EXECUTE,
                    rawQuery = trimmed,
                    parameters = mapOf("routine" to "bedtime routine")
                )
            }
        }

        // 4. Volume Control
        if (lower.contains("volume") || lower.contains("awaaz") || lower.contains("awaz") || lower.contains("silent") || lower.contains("vibrate")) {
            val percentage = extractPercentage(lower)
            val isSilent = lower.contains("silent") || lower.contains("mute") || lower.contains("shant")
            val isVibrate = lower.contains("vibrate") || lower.contains("vibration")
            val isNormal = lower.contains("unmute") || lower.contains("normal")
            val isDown = lower.contains("kam") || lower.contains("down") || lower.contains("ghatao")
            val isUp = lower.contains("badhao") || lower.contains("up") || lower.contains("tez")

            val params = mutableMapOf<String, Any?>()
            when {
                isSilent -> params["mode"] = "silent"
                isVibrate -> params["mode"] = "vibrate"
                isNormal -> params["mode"] = "normal"
                percentage != null -> params["percentage"] = percentage
                isDown -> params["direction"] = "down"
                isUp -> params["direction"] = "up"
                else -> params["percentage"] = 50
            }
            return ActionPlan(intent = ActionType.VOLUME_CONTROL, rawQuery = trimmed, parameters = params)
        }

        // 5. Brightness Control
        if (lower.contains("brightness") || lower.contains("roshni") || lower.contains("display light")) {
            val percentage = extractPercentage(lower) ?: 50
            return ActionPlan(
                intent = ActionType.BRIGHTNESS_CONTROL,
                rawQuery = trimmed,
                parameters = mapOf("percentage" to percentage)
            )
        }

        // 6. Flashlight / Torch
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("flash light") || lower.contains("flaslight")) {
            val isOff = lower.contains("off") || lower.contains("band") || lower.contains("bujhao")
            return ActionPlan(
                intent = ActionType.FLASHLIGHT_CONTROL,
                rawQuery = trimmed,
                parameters = mapOf("state" to !isOff)
            )
        }

        // 7. Bluetooth
        if (lower.contains("bluetooth")) {
            val isOff = lower.contains("off") || lower.contains("band")
            return ActionPlan(
                intent = ActionType.BLUETOOTH_CONTROL,
                rawQuery = trimmed,
                parameters = mapOf("state" to !isOff)
            )
        }

        // 8. Wi-Fi
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("wi fi")) {
            val isOff = lower.contains("off") || lower.contains("band")
            return ActionPlan(
                intent = ActionType.WIFI_CONTROL,
                rawQuery = trimmed,
                parameters = mapOf("state" to !isOff)
            )
        }

        // 9. Screenshot
        if (lower.contains("screenshot") || lower.contains("screen shot") || (lower.contains("screen") && lower.contains("capture"))) {
            return ActionPlan(intent = ActionType.SCREENSHOT, rawQuery = trimmed)
        }

        // 10. Camera
        if (lower.contains("camera") || (lower.contains("photo") && (lower.contains("khicho") || lower.contains("kheecho") || lower.contains("click")))) {
            return ActionPlan(intent = ActionType.CAMERA_LAUNCH, rawQuery = trimmed)
        }

        // 11. Notification reading
        if (lower.contains("notification") || lower.contains("suchna") || lower.contains("notif")) {
            val isDismiss = lower.contains("dismiss") || lower.contains("clear") || lower.contains("hatao") || lower.contains("band")
            return ActionPlan(
                intent = ActionType.NOTIFICATION_READ,
                rawQuery = trimmed,
                parameters = mapOf("subAction" to if (isDismiss) "dismiss" else "read")
            )
        }

        // 12. Call contact: "Papa ko call karo", "Call Rahul", "Mummy ko phone lagao"
        if (lower.contains("call") || lower.contains("phone lagao") || lower.contains("dial")) {
            val contact = extractContactName(lower)
            return ActionPlan(
                intent = ActionType.CALL_CONTACT,
                rawQuery = trimmed,
                parameters = mapOf("target" to contact)
            )
        }

        // 13. Send message / SMS: "Rahul ko message bhejo ki main 10 minute late hoon"
        if (lower.contains("message") || lower.contains("sms") || lower.contains("sandesh")) {
            val (recipient, msgText) = extractMessageDetails(lower)
            return ActionPlan(
                intent = ActionType.SEND_SMS,
                rawQuery = trimmed,
                parameters = mapOf("recipient" to recipient, "message" to msgText)
            )
        }

        // 14. Alarm: "Kal subah 6 baje alarm laga do", "7 am alarm"
        if (lower.contains("alarm") || (lower.contains("utha dena") || lower.contains("jagana"))) {
            val (hour, minute) = extractAlarmTime(lower)
            return ActionPlan(
                intent = ActionType.SET_ALARM,
                rawQuery = trimmed,
                parameters = mapOf("hour" to hour, "minute" to minute, "message" to "Jarvis Alarm")
            )
        }

        // 15. Timer: "5 minute ka timer", "timer 10 second"
        if (lower.contains("timer")) {
            val seconds = extractTimerSeconds(lower)
            return ActionPlan(
                intent = ActionType.SET_TIMER,
                rawQuery = trimmed,
                parameters = mapOf("seconds" to seconds, "message" to "Jarvis Timer")
            )
        }

        // 16. Navigation / Maps: "Google Maps mein ghar ka route kholo", "Raipur railway station ka navigation start karo"
        if (lower.contains("navigation") || lower.contains("route") || lower.contains("rasta") || (lower.contains("maps") && !lower.contains("kholo"))) {
            val destination = extractDestination(lower)
            return ActionPlan(
                intent = ActionType.MAPS_NAVIGATION,
                rawQuery = trimmed,
                parameters = mapOf("destination" to destination)
            )
        }

        // 17. Media Controls: "Music chalao", "play", "pause", "next song", "previous song"
        if (lower.contains("music") || lower.contains("song") || lower.contains("gana") || lower == "play" || lower == "pause" || lower.contains("next") || lower.contains("previous")) {
            val cmd = when {
                lower.contains("pause") || lower.contains("roko") -> "pause"
                lower.contains("next") || lower.contains("agla") -> "next"
                lower.contains("previous") || lower.contains("pichla") -> "previous"
                else -> "play"
            }
            return ActionPlan(
                intent = ActionType.MEDIA_CONTROL,
                rawQuery = trimmed,
                parameters = mapOf("command" to cmd)
            )
        }

        // 18. Files / Photos Search: "PDF files dikhao", "Kal ki photos dikhao", "Downloads mein UPSC wali file dhundo"
        if (lower.contains("file") || lower.contains("pdf") || lower.contains("photo") || lower.contains("download")) {
            val category = when {
                lower.contains("photo") || lower.contains("tasveer") -> "photo"
                lower.contains("pdf") -> "pdf"
                else -> "document"
            }
            return ActionPlan(
                intent = ActionType.FILE_SEARCH,
                rawQuery = trimmed,
                parameters = mapOf("query" to trimmed, "category" to category)
            )
        }

        // 19. General Web Search: "Google par UPSC current affairs search karo", "Search karo Chhattisgarh ka weather"
        if (lower.contains("search") || lower.contains("dhundo") || lower.contains("khojo") || lower.contains("weather") || lower.contains("mausam")) {
            val cleanQuery = extractGeneralSearchQuery(lower)
            return ActionPlan(
                intent = ActionType.WEB_SEARCH,
                rawQuery = trimmed,
                parameters = mapOf("query" to cleanQuery, "platform" to "google")
            )
        }

        // 20. Open App: "WhatsApp kholo", "Instagram open karo", "Settings kholo"
        if (lower.contains("kholo") || lower.contains("open") || lower.contains("launch") || lower.contains("chalao")) {
            val appTarget = extractAppTarget(lower)
            return ActionPlan(
                intent = ActionType.OPEN_APP,
                rawQuery = trimmed,
                parameters = mapOf("target" to appTarget)
            )
        }

        // 21. Accessibility navigation: "back", "home", "recents"
        if (lower.contains("home screen") || lower == "home" || lower.contains("mukhya page")) {
            return ActionPlan(intent = ActionType.ACCESSIBILITY_GESTURE, rawQuery = trimmed, parameters = mapOf("gesture" to "home"))
        }
        if (lower.contains("piche jao") || lower == "back" || lower.contains("wapas")) {
            return ActionPlan(intent = ActionType.ACCESSIBILITY_GESTURE, rawQuery = trimmed, parameters = mapOf("gesture" to "back"))
        }

        // Fallback default: Search on web
        return ActionPlan(
            intent = ActionType.WEB_SEARCH,
            rawQuery = trimmed,
            confidence = 0.5f,
            parameters = mapOf("query" to trimmed, "platform" to "google")
        )
    }

    private fun isSensitiveAction(text: String): Boolean {
        val dangerousKeywords = listOf(
            "upi", "pay", "payment", "bhej do", "transfer", "delete file",
            "format", "factory reset", "password", "otp", "pin", "khata band"
        )
        return dangerousKeywords.any { text.contains(it) }
    }

    private fun extractPercentage(text: String): Int? {
        val match = Regex("""(\d+)\s*(?:%|percent|pratishat)""").find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractAlarmTime(text: String): Pair<Int, Int> {
        val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(?:baje|am|pm|subah|shaam|dopahar)?""")
        val match = timeRegex.find(text)
        var hour = match?.groupValues?.get(1)?.toIntOrNull() ?: 7
        val minute = match?.groupValues?.get(2)?.toIntOrNull() ?: 0

        if ((text.contains("shaam") || text.contains("raat") || text.contains("pm")) && hour < 12) {
            hour += 12
        }
        return Pair(hour, minute)
    }

    private fun extractTimerSeconds(text: String): Int {
        val minMatch = Regex("""(\d+)\s*(?:minute|min|m)""").find(text)
        if (minMatch != null) {
            return (minMatch.groupValues[1].toIntOrNull() ?: 5) * 60
        }
        val secMatch = Regex("""(\d+)\s*(?:second|sec|s)""").find(text)
        if (secMatch != null) {
            return secMatch.groupValues[1].toIntOrNull() ?: 30
        }
        return 300
    }

    private fun extractContactName(text: String): String {
        val koMatch = Regex("""([a-zA-Z\u0900-\u097F]+)\s*ko\s*(?:call|phone)""").find(text)
        if (koMatch != null) return koMatch.groupValues[1].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        val callMatch = Regex("""call\s+([a-zA-Z\u0900-\u097F]+)""").find(text)
        if (callMatch != null) return callMatch.groupValues[1].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        return text.replace("call", "").replace("karo", "").replace("phone", "").replace("lagao", "").trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private fun extractMessageDetails(text: String): Pair<String, String> {
        val regex = Regex("""([a-zA-Z\u0900-\u097F]+)\s*ko\s*message\s*bhejo\s*(?:ki\s*)?(.*)""")
        val match = regex.find(text)
        return if (match != null) {
            val recipient = match.groupValues[1].trim()
            val msg = match.groupValues[2].trim()
            Pair(recipient, msg.ifBlank { "Hello" })
        } else {
            Pair("Contact", text)
        }
    }

    private fun extractDestination(text: String): String {
        return text.replace("google maps mein", "")
            .replace("maps", "")
            .replace("mein", "")
            .replace("ka route kholo", "")
            .replace("navigation start karo", "")
            .replace("navigation", "")
            .replace("route", "")
            .replace("kholo", "")
            .trim()
    }

    private fun extractSearchQuery(text: String, platform: String): String {
        return text.replace(platform, "")
            .replace("kholo", "")
            .replace("aur", "")
            .replace("search karo", "")
            .replace("search", "")
            .replace("dhundo", "")
            .replace("par", "")
            .trim()
    }

    private fun extractGeneralSearchQuery(text: String): String {
        return text.replace("search karo", "")
            .replace("search", "")
            .replace("google par", "")
            .replace("google", "")
            .replace("dhundo", "")
            .replace("khojo", "")
            .replace("batao", "")
            .trim()
    }

    private fun extractAppTarget(text: String): String {
        return text.replace("open karo", "")
            .replace("launch karo", "")
            .replace("chalao", "")
            .replace("kholo", "")
            .replace("open", "")
            .replace("launch", "")
            .replace(Regex("""\bapps?\b|\bapplication\b"""), "")
            .trim()
    }
}
