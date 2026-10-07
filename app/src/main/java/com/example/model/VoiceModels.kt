package com.example.model

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    EXECUTING,
    SPEAKING,
    ERROR
}

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val aliases: List<String> = emptyList()
)

data class NotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isClearable: Boolean
)

data class ContactInfo(
    val id: String,
    val name: String,
    val number: String
)

data class AutomationStep(
    val actionType: ActionType,
    val parameters: Map<String, String> = emptyMap(),
    val delayMs: Long = 500
)
