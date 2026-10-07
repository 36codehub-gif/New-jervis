package com.example.model

enum class ActionType(val title: String) {
    OPEN_APP("Open Application"),
    CALL_CONTACT("Call Contact"),
    SEND_SMS("Send SMS"),
    COMPOSE_MESSAGE("Compose Message"),
    SET_ALARM("Set Alarm"),
    SET_TIMER("Set Timer"),
    CREATE_REMINDER("Create Reminder"),
    CALENDAR_EVENT("Calendar Schedule"),
    VOLUME_CONTROL("Volume Adjustment"),
    BRIGHTNESS_CONTROL("Brightness Control"),
    BLUETOOTH_CONTROL("Bluetooth Control"),
    WIFI_CONTROL("Wi-Fi Control"),
    FLASHLIGHT_CONTROL("Flashlight / Torch"),
    SCREENSHOT("Capture Screenshot"),
    CAMERA_LAUNCH("Launch Camera"),
    NOTIFICATION_READ("Read Recent Notifications"),
    NOTIFICATION_DISMISS("Dismiss Notifications"),
    OPEN_SETTINGS("System Settings"),
    WEB_SEARCH("Web / Online Search"),
    MAPS_NAVIGATION("Maps & Route Navigation"),
    MEDIA_CONTROL("Media Playback Control"),
    FILE_SEARCH("File & Document Search"),
    CLIPBOARD_ACTION("Clipboard Action"),
    ACCESSIBILITY_GESTURE("Accessibility Screen Gesture"),
    ROUTINE_EXECUTE("Routine Automation"),
    UNKNOWN("Unknown Command")
}

enum class ActionStatus {
    SUCCESS,
    FAILED,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    NEEDS_CONFIRMATION
}
