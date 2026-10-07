package com.example.model

import android.content.Intent

data class ActionResult(
    val status: ActionStatus,
    val message: String,
    val speechResponse: String,
    val details: String? = null,
    val recoveryIntent: Intent? = null,
    val requiredPermission: String? = null,
    val payload: Any? = null
) {
    companion object {
        fun success(message: String, speech: String, details: String? = null, payload: Any? = null) =
            ActionResult(
                status = ActionStatus.SUCCESS,
                message = message,
                speechResponse = speech,
                details = details,
                payload = payload
            )

        fun failed(message: String, speech: String, details: String? = null) =
            ActionResult(
                status = ActionStatus.FAILED,
                message = message,
                speechResponse = speech,
                details = details
            )

        fun permissionRequired(permission: String, message: String, speech: String, settingsIntent: Intent? = null) =
            ActionResult(
                status = ActionStatus.PERMISSION_REQUIRED,
                message = message,
                speechResponse = speech,
                requiredPermission = permission,
                recoveryIntent = settingsIntent
            )

        fun unsupported(message: String, speech: String, settingsIntent: Intent? = null) =
            ActionResult(
                status = ActionStatus.UNSUPPORTED,
                message = message,
                speechResponse = speech,
                recoveryIntent = settingsIntent
            )

        fun needsConfirmation(message: String, speech: String, details: String? = null, payload: Any? = null) =
            ActionResult(
                status = ActionStatus.NEEDS_CONFIRMATION,
                message = message,
                speechResponse = speech,
                details = details,
                payload = payload
            )
    }
}
