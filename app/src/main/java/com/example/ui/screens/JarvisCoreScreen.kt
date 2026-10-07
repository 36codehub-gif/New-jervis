package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ActionResult
import com.example.model.ActionStatus
import com.example.model.VoiceState
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.theme.*
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun JarvisCoreScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsState()
    val lastTranscript by viewModel.lastTranscript.collectAsState()
    val partialTranscript by viewModel.partialTranscript.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val currentPlan by viewModel.currentPlan.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val continuousListening by viewModel.continuousListening.collectAsState()

    var manualTextInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val quickCommands = listOf(
        "WhatsApp kholo",
        "Papa ko call karo",
        "YouTube kholo aur UPSC search karo",
        "Torch on karo",
        "Phone silent kar",
        "Volume 50 percent kar",
        "Kal subah 6 baje alarm laga do",
        "Screenshot le",
        "Recent notification batao",
        "Google Maps mein ghar ka route kholo",
        "Study mode banao",
        "Bluetooth on karo"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Futuristic Top Telemetry Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisBorderCyan, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (voiceState == VoiceState.LISTENING) JarvisCyanPrimary else JarvisSuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CORE ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisCyanLight,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Continuous Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = continuousListening,
                        onCheckedChange = { viewModel.toggleContinuousListening() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisCyanPrimary,
                            checkedTrackColor = JarvisSurfaceVariantDark
                        ),
                        modifier = Modifier
                            .height(24.dp)
                            .testTag("continuous_mode_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Arc Reactor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            ArcReactorVisualizer(
                voiceState = voiceState,
                rmsLevel = rmsLevel,
                onClick = {
                    if (voiceState == VoiceState.LISTENING) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                }
            )
        }

        // Voice State Label & HUD Status
        val statusText = when (voiceState) {
            VoiceState.LISTENING -> "JARVIS IS LISTENING..."
            VoiceState.PROCESSING -> "ANALYZING COMMAND INTENT..."
            VoiceState.EXECUTING -> "EXECUTING SYSTEM ACTION..."
            VoiceState.SPEAKING -> "JARVIS SPEAKING..."
            VoiceState.ERROR -> "SYSTEM ALERT"
            VoiceState.IDLE -> "TAP CORE OR SAY 'HEY JARVIS'"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelLarge,
            color = when (voiceState) {
                VoiceState.LISTENING -> JarvisCyanPrimary
                VoiceState.PROCESSING, VoiceState.EXECUTING -> JarvisAmberAccent
                VoiceState.SPEAKING -> JarvisSuccessGreen
                VoiceState.ERROR -> JarvisErrorRed
                VoiceState.IDLE -> JarvisTextSecondary
            },
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Live Transcript / Last Command Display
        val currentDisplay = partialTranscript.ifBlank { lastTranscript }
        if (currentDisplay.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariantDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JarvisBorderCyan, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Transcript",
                        tint = JarvisCyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "VOICE INPUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisTextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "\"$currentDisplay\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Action Execution Result Card
        lastResult?.let { result ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (result.status) {
                        ActionStatus.SUCCESS -> Color(0xFF0C241F)
                        ActionStatus.FAILED -> Color(0xFF281118)
                        ActionStatus.PERMISSION_REQUIRED -> Color(0xFF2B200C)
                        ActionStatus.UNSUPPORTED -> Color(0xFF211D2B)
                        ActionStatus.NEEDS_CONFIRMATION -> Color(0xFF2B200C)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = when (result.status) {
                            ActionStatus.SUCCESS -> JarvisSuccessGreen
                            ActionStatus.FAILED -> JarvisErrorRed
                            ActionStatus.PERMISSION_REQUIRED, ActionStatus.NEEDS_CONFIRMATION -> JarvisAmberAccent
                            ActionStatus.UNSUPPORTED -> JarvisCyanLight
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPlan?.intent?.title ?: "SYSTEM ACTION",
                            style = MaterialTheme.typography.labelMedium,
                            color = JarvisTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Badge(
                            containerColor = when (result.status) {
                                ActionStatus.SUCCESS -> JarvisSuccessGreen
                                ActionStatus.FAILED -> JarvisErrorRed
                                ActionStatus.PERMISSION_REQUIRED -> JarvisAmberAccent
                                ActionStatus.UNSUPPORTED -> JarvisCyanLight
                                ActionStatus.NEEDS_CONFIRMATION -> JarvisAmberAccent
                            }
                        ) {
                            Text(
                                text = result.status.name,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = JarvisTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!result.speechResponse.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Jarvis: \"${result.speechResponse}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisCyanLight,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    if (!result.details.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = result.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextSecondary
                        )
                    }

                    // Recovery settings button if available
                    result.recoveryIntent?.let { recoveryIntent ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(recoveryIntent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisAmberAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Required Settings Screen")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Command Suggestions Chips
        Text(
            text = "VOICE COMMAND SUGGESTIONS (HINDI & ENGLISH)",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisTextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCommands.forEach { cmd ->
                AssistChip(
                    onClick = { viewModel.executeManualCommand(cmd) },
                    label = { Text(cmd, fontSize = 12.sp, color = JarvisCyanLight) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = JarvisAmberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = JarvisSurfaceDark
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        borderColor = JarvisBorderCyan,
                        enabled = true
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("quick_chip_$cmd")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Command Text Input
        OutlinedTextField(
            value = manualTextInput,
            onValueChange = { manualTextInput = it },
            placeholder = { Text("Or type command (e.g., 'Papa ko call karo')...", color = JarvisTextMuted) },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (manualTextInput.isNotBlank()) {
                            viewModel.executeManualCommand(manualTextInput)
                            manualTextInput = ""
                        }
                    },
                    modifier = Modifier.testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = JarvisCyanPrimary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (manualTextInput.isNotBlank()) {
                        viewModel.executeManualCommand(manualTextInput)
                        manualTextInput = ""
                    }
                }
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = JarvisSurfaceDark,
                unfocusedContainerColor = JarvisSurfaceDark,
                focusedBorderColor = JarvisCyanPrimary,
                unfocusedBorderColor = JarvisBorderCyan,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("command_input_field")
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
