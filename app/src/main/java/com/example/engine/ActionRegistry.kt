package com.example.engine

import com.example.actions.*
import com.example.model.ActionType
import com.example.voice.InstalledAppManager

class ActionRegistry(appManager: InstalledAppManager) {

    private val actions = mutableMapOf<ActionType, Action>()

    init {
        register(OpenAppAction(appManager))
        register(CloseAppAction())
        register(LaunchActivityAction())
        register(AppSearchAction(appManager))
        register(CallContactAction())
        register(SendSmsAction())
        register(ComposeMessageAction())
        register(SetAlarmAction())
        register(SetTimerAction())
        register(CalendarAction())
        register(VolumeAction())
        register(BrightnessAction())
        register(FlashlightAction())
        register(BluetoothAction())
        register(WiFiAction())
        register(ScreenshotAction())
        register(CameraAction())
        register(OpenSettingsAction())
        register(WebSearchAction())
        register(MapsNavigationAction())
        register(MediaControlAction())
        register(NotificationAction())
        register(FileSearchAction())
        register(ClipboardAction())
        register(AccessibilityAction())
    }

    fun register(action: Action) {
        actions[action.type] = action
    }

    fun getAction(type: ActionType): Action? = actions[type]

    fun getAllActions(): List<Action> = actions.values.toList()
}
