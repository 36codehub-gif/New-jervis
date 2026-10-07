package com.example.data.repository

import com.example.data.dao.JarvisDao
import com.example.data.entity.CommandHistoryEntity
import com.example.data.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val dao: JarvisDao) {

    val allHistory: Flow<List<CommandHistoryEntity>> = dao.getAllHistory()
    val allRoutines: Flow<List<RoutineEntity>> = dao.getAllRoutines()

    suspend fun recordHistory(
        rawCommand: String,
        intentName: String,
        status: String,
        resultMessage: String,
        durationMs: Long
    ): Long {
        return dao.insertHistory(
            CommandHistoryEntity(
                rawCommand = rawCommand,
                intentName = intentName,
                status = status,
                resultMessage = resultMessage,
                timestamp = System.currentTimeMillis(),
                executionDurationMs = durationMs
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun deleteHistory(id: Long) {
        dao.deleteHistoryById(id)
    }

    suspend fun insertRoutine(routine: RoutineEntity) = dao.insertRoutine(routine)

    suspend fun updateRoutine(routine: RoutineEntity) = dao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: RoutineEntity) = dao.deleteRoutine(routine)

    suspend fun getActiveRoutines(): List<RoutineEntity> = dao.getActiveRoutines()

    suspend fun seedDefaultRoutinesIfEmpty() {
        val existing = dao.getActiveRoutines()
        if (existing.isEmpty()) {
            val defaults = listOf(
                RoutineEntity(
                    title = "Study Mode",
                    triggerPhrase = "study mode",
                    description = "Phone silent karo, volume 0% karo, aur 45 minute ka study timer chalao.",
                    stepsJson = """[{"actionType":"VOLUME_CONTROL","parameters":{"mode":"silent","percentage":"0"}},{"actionType":"SET_TIMER","parameters":{"seconds":"2700","message":"Study Session"}}]""",
                    iconName = "school"
                ),
                RoutineEntity(
                    title = "Morning Routine",
                    triggerPhrase = "morning routine",
                    description = "Media volume 60% karo, recent notifications batao aur calendar open karo.",
                    stepsJson = """[{"actionType":"VOLUME_CONTROL","parameters":{"percentage":"60"}},{"actionType":"NOTIFICATION_READ","parameters":{}},{"actionType":"CALENDAR_EVENT","parameters":{}}]""",
                    iconName = "wb_sunny"
                ),
                RoutineEntity(
                    title = "Workout Mode",
                    triggerPhrase = "workout mode",
                    description = "Music app kholo, media volume 80% karo, aur workout timer lagao.",
                    stepsJson = """[{"actionType":"VOLUME_CONTROL","parameters":{"percentage":"80"}},{"actionType":"MEDIA_CONTROL","parameters":{"command":"play"}},{"actionType":"SET_TIMER","parameters":{"seconds":"1800","message":"Workout Workout"}}]""",
                    iconName = "fitness_center"
                ),
                RoutineEntity(
                    title = "Bedtime Routine",
                    triggerPhrase = "bedtime routine",
                    description = "Phone silent karo, screen brightness kam karo, aur subah 6 baje alarm set karo.",
                    stepsJson = """[{"actionType":"VOLUME_CONTROL","parameters":{"mode":"silent"}},{"actionType":"SET_ALARM","parameters":{"hour":"6","minute":"0","message":"Subah ki uthne ka samay"}}]""",
                    iconName = "bedtime"
                )
            )
            for (routine in defaults) {
                dao.insertRoutine(routine)
            }
        }
    }
}
