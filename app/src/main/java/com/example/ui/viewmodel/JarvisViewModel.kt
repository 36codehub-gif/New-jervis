package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.data.entity.CommandHistoryEntity
import com.example.data.entity.RoutineEntity
import com.example.model.ActionPlan
import com.example.model.ActionResult
import com.example.model.ActionStatus
import com.example.model.VoiceState
import com.example.permission.PermissionItem
import com.example.service.JarvisVoiceService
import com.example.voice.VoiceRecognitionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val repository = app.repository
    private val actionExecutor = app.actionExecutor
    private val permissionManager = app.permissionManager

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _lastResult = MutableStateFlow<ActionResult?>(null)
    val lastResult: StateFlow<ActionResult?> = _lastResult.asStateFlow()

    private val _currentPlan = MutableStateFlow<ActionPlan?>(null)
    val currentPlan: StateFlow<ActionPlan?> = _currentPlan.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ActionPlan?>(null)
    val pendingConfirmation: StateFlow<ActionPlan?> = _pendingConfirmation.asStateFlow()

    private val _continuousListening = MutableStateFlow(false)
    val continuousListening: StateFlow<Boolean> = _continuousListening.asStateFlow()

    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    val historyList: StateFlow<List<CommandHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routinesList: StateFlow<List<RoutineEntity>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var voiceManager: VoiceRecognitionManager? = null

    init {
        refreshPermissions()
        setupVoiceRecognizer()
    }

    private fun setupVoiceRecognizer() {
        voiceManager = VoiceRecognitionManager(
            context = app,
            listener = object : VoiceRecognitionManager.Listener {
                override fun onReady() {
                    _voiceState.value = VoiceState.LISTENING
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsLevel.value = rmsdB
                }

                override fun onPartialResult(text: String) {
                    _partialTranscript.value = text
                }

                override fun onFinalResult(text: String) {
                    _partialTranscript.value = ""
                    _lastTranscript.value = text
                    _rmsLevel.value = 0f
                    handleSpeechInput(text)
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    _rmsLevel.value = 0f
                    _voiceState.value = VoiceState.ERROR
                    _lastResult.value = ActionResult.failed(
                        "Voice error: $errorMessage",
                        "Awaz samajh nahi aayi. Kripya dobara bolein."
                    )
                }
            }
        )
    }

    fun startListening() {
        _lastResult.value = null
        _voiceState.value = VoiceState.LISTENING
        voiceManager?.startListening(isHindiPreferred = true)
    }

    fun stopListening() {
        voiceManager?.stopListening()
        _voiceState.value = VoiceState.IDLE
        _rmsLevel.value = 0f
    }

    fun handleSpeechInput(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) {
            _voiceState.value = VoiceState.IDLE
            return
        }

        // Check for wake word prefix
        val stripped = if (clean.startsWith("hey jarvis", ignoreCase = true)) {
            clean.substringAfter("hey jarvis").trim()
        } else if (clean.startsWith("jarvis", ignoreCase = true)) {
            clean.substringAfter("jarvis").trim()
        } else {
            clean
        }

        val queryToExecute = stripped.ifBlank { clean }

        executeCommandInternal(queryToExecute)
    }

    fun executeManualCommand(text: String) {
        _lastTranscript.value = text
        executeCommandInternal(text)
    }

    private fun executeCommandInternal(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _voiceState.value = VoiceState.PROCESSING
            val (plan, result) = actionExecutor.processCommand(query)
            _currentPlan.value = plan
            _lastResult.value = result

            if (result.status == ActionStatus.NEEDS_CONFIRMATION) {
                _pendingConfirmation.value = plan
                _voiceState.value = VoiceState.IDLE
            } else {
                _voiceState.value = VoiceState.SPEAKING
                // Automatically reset to idle after brief period
                kotlinx.coroutines.delay(2000)
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    fun confirmPendingAction() {
        val plan = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch(Dispatchers.IO) {
            _voiceState.value = VoiceState.EXECUTING
            val result = actionExecutor.executeConfirmedPlan(plan)
            _lastResult.value = result
            _voiceState.value = VoiceState.SPEAKING
            kotlinx.coroutines.delay(2000)
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun cancelPendingAction() {
        _pendingConfirmation.value = null
        _lastResult.value = ActionResult.failed(
            "Action cancelled by user",
            "Action cancel kar diya gaya hai."
        )
        _voiceState.value = VoiceState.IDLE
    }

    fun toggleContinuousListening() {
        val newState = !_continuousListening.value
        _continuousListening.value = newState
        if (newState) {
            JarvisVoiceService.start(app)
        } else {
            JarvisVoiceService.stop(app)
        }
    }

    fun runRoutine(routine: RoutineEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _voiceState.value = VoiceState.EXECUTING
            val res = actionExecutor.executeRoutineJson(routine.stepsJson, routine.title)
            _lastResult.value = res
            _lastTranscript.value = "Routine: ${routine.title}"
            _voiceState.value = VoiceState.SPEAKING
            kotlinx.coroutines.delay(2000)
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun toggleRoutine(routine: RoutineEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRoutine(routine.copy(isEnabled = !routine.isEnabled))
        }
    }

    fun saveRoutine(title: String, trigger: String, description: String, stepsJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRoutine(
                RoutineEntity(
                    title = title,
                    triggerPhrase = trigger,
                    description = description,
                    stepsJson = stepsJson
                )
            )
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRoutine(routine)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    fun refreshPermissions() {
        _permissions.value = permissionManager.getPermissionStatusList()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.stopListening()
    }
}
