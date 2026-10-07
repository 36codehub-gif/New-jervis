# Android Jarvis

A voice-controlled Android assistant built with Kotlin and Jetpack Compose that executes real actions on the user's phone instead of merely replying with text.

---

## Key Highlights

- **Intent-Driven Voice Assistant**: Understands commands in Hindi, English, and Hinglish. Supports on-demand listening and optional continuous wake-word detection ("Hey Jarvis").
- **Real Android Action Engine**:
  - `OpenAppAction`, `CloseAppAction`, `LaunchActivityAction`, `AppSearchAction`
  - `CallContactAction`, `SendSmsAction`, `ComposeMessageAction`
  - `SetAlarmAction`, `SetTimerAction`, `CalendarAction`
  - `VolumeAction`, `BrightnessAction`, `FlashlightAction`, `BluetoothAction`, `WiFiAction`
  - `ScreenshotAction`, `CameraAction`, `NotificationAction`
  - `WebSearchAction`, `MapsNavigationAction`, `MediaControlAction`, `FileSearchAction`, `ClipboardAction`, `AccessibilityAction`
- **Official AccessibilityService (`JarvisAccessibilityService`)**:
  - Hands-free navigation gestures (Back, Home, Recents, Notifications shade, Quick Settings)
  - Native screenshot capture via `takeScreenshot()`
  - On-screen node text search, click automation, and field typing
- **NotificationListenerService (`JarvisNotificationListenerService`)**:
  - Intercepts and parses incoming alerts
  - Reads notifications aloud upon command ("WhatsApp par Rahul ka message aaya hai")
  - Allows dismissing actionable alerts
- **Automation & Routines Engine**:
  - Visual routine builder backed by Room Database
  - Pre-seeded routines: **Study Mode**, **Morning Routine**, **Workout Mode**, and **Bedtime Routine**
- **Security & Confirmation Framework**:
  - Mandatory confirmation modal before high-risk actions (UPI transactions, money transfers, file deletions, security alterations)
  - Never stores credentials, PINs, OTPs, or passwords
- **Offline First**:
  - Core device operations (volume, torch, alarms, timers, app launches, contacts, navigation, routines) function without internet connectivity

---

## Tech Stack

- **Language:** Kotlin 2.2
- **UI Toolkit:** Jetpack Compose with Material Design 3
- **Architecture:** Clean MVVM (Model-View-ViewModel) + Coroutines & Flow
- **Local Persistence:** Room Database with KSP code generation
- **Voice Stack:** Android SpeechRecognizer + TextToSpeech (Hindi & English locale support)
- **Background Services:** Foreground Service with Ongoing Microphone notification, AccessibilityService, NotificationListenerService

---

## Command Reference (Hinglish, Hindi & English)

| Intent / Feature | Example Voice Commands |
| :--- | :--- |
| **Open Apps** | *"WhatsApp kholo"*, *"Instagram open karo"*, *"Calculator launch karo"* |
| **Call Contact** | *"Papa ko call karo"*, *"Call Rahul"*, *"Mummy ko phone lagao"* |
| **Compound Search** | *"YouTube kholo aur UPSC current affairs search karo"* |
| **Volume & Modes** | *"Volume 50 percent kar"*, *"Phone silent kar"*, *"Volume badhao"*, *"Vibrate karo"* |
| **Brightness** | *"Brightness 40 percent kar do"*, *"Roshni kam karo"* |
| **Flashlight / Torch** | *"Torch on karo"*, *"Flashlight band karo"* |
| **Clock & Alarms** | *"Kal subah 6 baje alarm laga do"*, *"5 minute ka timer lagao"* |
| **Screenshot** | *"Screenshot le"* (via Accessibility service) |
| **Notifications** | *"Recent notification batao"*, *"Last notification kya aayi?"* |
| **Maps & Navigation**| *"Google Maps mein ghar ka route kholo"*, *"Raipur railway station ka navigation shuru karo"* |
| **Media Playback** | *"Music chalao"*, *"Pause"*, *"Next song"*, *"Previous song"* |
| **File & Photo Search**| *"PDF files dikhao"*, *"Downloads mein UPSC wali file dhundo"*, *"Kal ki photos dikhao"* |
| **Routines** | *"Study mode banao"*, *"Morning routine start karo"*, *"Workout mode"* |
| **Security Confirmation**| *"UPI se Rahul ko 500 bhej do"* → Triggers Authorization Dialog |

---

## Exact Build & Run Instructions

### Prerequisites
- JDK 17 or JDK 21
- Android Studio Ladybug / Meerkat or Android SDK Build Tools (API 36)

### Build Commands

1. **Verify Compilation:**
   ```bash
   gradle assembleDebug
   ```

2. **Execute Local JVM Tests (Robolectric & Unit Tests):**
   ```bash
   gradle :app:testDebugUnitTest
   ```

3. **Install Debug APK onto Connected Device / Emulator:**
   ```bash
   gradle installDebug
   ```

### Post-Install Permissions Setup
1. Open **Android Jarvis** on device.
2. Grant runtime permissions for Microphone, Phone Calls, Contacts, SMS, and Camera.
3. Open the **Services** tab:
   - Enable **Jarvis Accessibility Service** in Android Accessibility Settings.
   - Grant **Notification Access** in Android Notification Listener Settings.
   - Grant **Do Not Disturb (DND) Access** for automated silent mode routines.
