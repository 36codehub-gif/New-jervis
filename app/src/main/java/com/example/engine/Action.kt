package com.example.engine

import android.content.Context
import com.example.model.ActionResult
import com.example.model.ActionType

interface Action {
    val type: ActionType
    val displayName: String

    fun canExecute(context: Context): Boolean
    fun requiredPermissions(): List<String>
    suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult
    fun verifyResult(context: Context): Boolean
}
