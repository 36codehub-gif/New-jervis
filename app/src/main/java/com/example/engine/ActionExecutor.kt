package com.example.engine

import android.content.Context
import com.example.data.repository.JarvisRepository
import com.example.model.ActionPlan
import com.example.model.ActionResult
import com.example.model.ActionStatus
import com.example.model.ActionType
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

class ActionExecutor(
    private val context: Context,
    private val registry: ActionRegistry,
    private val repository: JarvisRepository,
    private val ttsManager: TextToSpeechManager,
    private val intentParser: IntentParser
) {

    suspend fun processCommand(rawCommand: String): Pair<ActionPlan, ActionResult> {
        val startTime = System.currentTimeMillis()
        val plan = intentParser.parse(rawCommand)

        // Safety check for explicit confirmation
        if (plan.requiresConfirmation) {
            val result = ActionResult.needsConfirmation(
                message = plan.confirmationMessage ?: "Explicit confirmation required for this action.",
                speech = plan.confirmationMessage ?: "Confirmation chahiye.",
                payload = plan
            )
            val duration = System.currentTimeMillis() - startTime
            repository.recordHistory(
                rawCommand = rawCommand,
                intentName = plan.intent.name,
                status = ActionStatus.NEEDS_CONFIRMATION.name,
                resultMessage = result.message,
                durationMs = duration
            )
            ttsManager.speak(result.speechResponse)
            return Pair(plan, result)
        }

        // Routine Automation Handler
        if (plan.intent == ActionType.ROUTINE_EXECUTE) {
            val routineTrigger = plan.parameters["routine"] as? String ?: ""
            val activeRoutines = repository.getActiveRoutines()
            val matched = activeRoutines.firstOrNull {
                it.triggerPhrase.contains(routineTrigger, ignoreCase = true) ||
                it.title.contains(routineTrigger, ignoreCase = true)
            }

            if (matched != null) {
                val routineResult = executeRoutineJson(matched.stepsJson, matched.title)
                val duration = System.currentTimeMillis() - startTime
                repository.recordHistory(
                    rawCommand = rawCommand,
                    intentName = "ROUTINE: ${matched.title}",
                    status = routineResult.status.name,
                    resultMessage = routineResult.message,
                    durationMs = duration
                )
                ttsManager.speak(routineResult.speechResponse)
                return Pair(plan, routineResult)
            }
        }

        // Standard Action Execution
        val action = registry.getAction(plan.intent)
        val result = if (action != null) {
            try {
                action.execute(context, plan.parameters)
            } catch (e: Exception) {
                ActionResult.failed("Execution error: ${e.message}", "Action pura karne mein dikkat aayi.")
            }
        } else {
            ActionResult.failed("Unsupported action: ${plan.intent.name}", "Is action ka support abhi uplabdh nahi hai.")
        }

        val duration = System.currentTimeMillis() - startTime
        repository.recordHistory(
            rawCommand = rawCommand,
            intentName = plan.intent.name,
            status = result.status.name,
            resultMessage = result.message,
            durationMs = duration
        )

        ttsManager.speak(result.speechResponse)
        return Pair(plan, result)
    }

    suspend fun executeConfirmedPlan(plan: ActionPlan): ActionResult {
        val startTime = System.currentTimeMillis()
        val action = registry.getAction(plan.intent)
        val result = if (action != null) {
            action.execute(context, plan.parameters)
        } else {
            ActionResult.success("Confirmed action acknowledged safely", "Action confirm ho gaya hai.")
        }

        val duration = System.currentTimeMillis() - startTime
        repository.recordHistory(
            rawCommand = plan.rawQuery,
            intentName = plan.intent.name,
            status = result.status.name,
            resultMessage = "CONFIRMED: ${result.message}",
            durationMs = duration
        )
        ttsManager.speak(result.speechResponse)
        return result
    }

    suspend fun executeRoutineJson(jsonStr: String, routineTitle: String): ActionResult {
        return try {
            val jsonArray = JSONArray(jsonStr)
            val executedSteps = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                val stepObj = jsonArray.getJSONObject(i)
                val actionTypeName = stepObj.getString("actionType")
                val actionType = try {
                    ActionType.valueOf(actionTypeName)
                } catch (_: Exception) {
                    ActionType.UNKNOWN
                }
                val paramsObj = stepObj.optJSONObject("parameters") ?: JSONObject()
                val paramsMap = mutableMapOf<String, Any?>()
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    paramsMap[k] = paramsObj.get(k)
                }

                val action = registry.getAction(actionType)
                if (action != null) {
                    val res = action.execute(context, paramsMap)
                    executedSteps.add("${action.displayName}: ${res.status}")
                }
                delay(300)
            }

            ActionResult.success(
                "Routine '$routineTitle' executed (${executedSteps.size} steps)",
                "'$routineTitle' safaltapoorvak chalu kar diya gaya hai",
                executedSteps.joinToString("\n")
            )
        } catch (e: Exception) {
            ActionResult.failed("Failed executing routine: ${e.message}", "Routine poora nahi ho paya")
        }
    }
}
