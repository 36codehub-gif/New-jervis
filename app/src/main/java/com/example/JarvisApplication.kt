package com.example

import android.app.Application
import com.example.data.JarvisDatabase
import com.example.data.repository.JarvisRepository
import com.example.engine.ActionExecutor
import com.example.engine.ActionRegistry
import com.example.engine.IntentParser
import com.example.permission.PermissionManager
import com.example.voice.InstalledAppManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JarvisApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: JarvisDatabase
        private set
    lateinit var repository: JarvisRepository
        private set
    lateinit var appManager: InstalledAppManager
        private set
    lateinit var actionRegistry: ActionRegistry
        private set
    lateinit var ttsManager: TextToSpeechManager
        private set
    lateinit var intentParser: IntentParser
        private set
    lateinit var actionExecutor: ActionExecutor
        private set
    lateinit var permissionManager: PermissionManager
        private set

    var isTtsSpeaking = false

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = JarvisDatabase.getInstance(this)
        repository = JarvisRepository(database.jarvisDao())
        appManager = InstalledAppManager(this)
        actionRegistry = ActionRegistry(appManager)
        ttsManager = TextToSpeechManager(this) { speaking ->
            isTtsSpeaking = speaking
        }
        intentParser = IntentParser()
        actionExecutor = ActionExecutor(this, actionRegistry, repository, ttsManager, intentParser)
        permissionManager = PermissionManager(this)

        applicationScope.launch {
            repository.seedDefaultRoutinesIfEmpty()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.shutdown()
    }

    companion object {
        lateinit var instance: JarvisApplication
            private set
    }
}
