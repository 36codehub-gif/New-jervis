package com.example.model

import java.util.UUID

data class ActionPlan(
    val id: String = UUID.randomUUID().toString(),
    val intent: ActionType,
    val confidence: Float = 1.0f,
    val parameters: Map<String, Any?> = emptyMap(),
    val rawQuery: String,
    val requiresConfirmation: Boolean = false,
    val confirmationMessage: String? = null
)
