package com.example

import com.example.engine.IntentParser
import com.example.model.ActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IntentParserTest {

    private lateinit var parser: IntentParser

    @Before
    fun setUp() {
        parser = IntentParser()
    }

    @Test
    fun testOpenAppCommands() {
        val plan1 = parser.parse("WhatsApp kholo")
        assertEquals(ActionType.OPEN_APP, plan1.intent)
        assertEquals("whatsapp", (plan1.parameters["target"] as String).lowercase())

        val plan2 = parser.parse("Instagram open karo")
        assertEquals(ActionType.OPEN_APP, plan2.intent)
        assertEquals("instagram", (plan2.parameters["target"] as String).lowercase())
    }

    @Test
    fun testCallContactCommands() {
        val plan = parser.parse("Papa ko call karo")
        assertEquals(ActionType.CALL_CONTACT, plan.intent)
        assertEquals("Papa", plan.parameters["target"])
    }

    @Test
    fun testCompoundYouTubeSearch() {
        val plan = parser.parse("YouTube kholo aur UPSC current affairs search karo")
        assertEquals(ActionType.WEB_SEARCH, plan.intent)
        assertEquals("youtube", plan.parameters["platform"])
        assertTrue((plan.parameters["query"] as String).contains("upsc current affairs"))
    }

    @Test
    fun testSystemControls() {
        val bluetoothPlan = parser.parse("Bluetooth on karo")
        assertEquals(ActionType.BLUETOOTH_CONTROL, bluetoothPlan.intent)

        val brightnessPlan = parser.parse("Brightness 40 percent kar do")
        assertEquals(ActionType.BRIGHTNESS_CONTROL, brightnessPlan.intent)
        assertEquals(40, brightnessPlan.parameters["percentage"])

        val volumePlan = parser.parse("Volume 50 percent kar")
        assertEquals(ActionType.VOLUME_CONTROL, volumePlan.intent)
        assertEquals(50, volumePlan.parameters["percentage"])

        val silentPlan = parser.parse("Phone silent kar")
        assertEquals(ActionType.VOLUME_CONTROL, silentPlan.intent)
        assertEquals("silent", silentPlan.parameters["mode"])

        val torchPlan = parser.parse("Torch on karo")
        assertEquals(ActionType.FLASHLIGHT_CONTROL, torchPlan.intent)
        assertEquals(true, torchPlan.parameters["state"])
    }

    @Test
    fun testClockAndAlarmCommands() {
        val alarmPlan = parser.parse("Kal subah 6 baje alarm laga do")
        assertEquals(ActionType.SET_ALARM, alarmPlan.intent)
        assertEquals(6, alarmPlan.parameters["hour"])

        val timerPlan = parser.parse("5 minute ka timer lagao")
        assertEquals(ActionType.SET_TIMER, timerPlan.intent)
        assertEquals(300, timerPlan.parameters["seconds"])
    }

    @Test
    fun testScreenshotAndNotifications() {
        val screenshotPlan = parser.parse("Screenshot le")
        assertEquals(ActionType.SCREENSHOT, screenshotPlan.intent)

        val notifPlan = parser.parse("Recent notification batao")
        assertEquals(ActionType.NOTIFICATION_READ, notifPlan.intent)
    }

    @Test
    fun testNavigationCommands() {
        val navPlan = parser.parse("Google Maps mein ghar ka route kholo")
        assertEquals(ActionType.MAPS_NAVIGATION, navPlan.intent)
        assertEquals("ghar", navPlan.parameters["destination"])
    }

    @Test
    fun testSensitiveActionRequiresConfirmation() {
        val paymentPlan = parser.parse("UPI se Rahul ko 500 bhej do")
        assertTrue(paymentPlan.requiresConfirmation)
        assertTrue(paymentPlan.confirmationMessage?.contains("500") == true)
    }

    @Test
    fun testRoutineTrigger() {
        val routinePlan = parser.parse("Study mode banao")
        assertEquals(ActionType.ROUTINE_EXECUTE, routinePlan.intent)
        assertEquals("study mode", routinePlan.parameters["routine"])
    }
}
