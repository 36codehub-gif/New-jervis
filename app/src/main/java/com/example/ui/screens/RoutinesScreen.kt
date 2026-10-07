package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RoutineEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun RoutinesScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val routines by viewModel.routinesList.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AUTOMATION ROUTINES",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisCyanPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Multi-step actions triggered by voice or tap",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary
                )
            }

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = JarvisCyanPrimary,
                contentColor = Color.Black,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("add_routine_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Routine")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (routines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No routines configured. Tap '+' to create your first automation.",
                    color = JarvisTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(routines, key = { it.id }) { routine ->
                    RoutineCard(
                        routine = routine,
                        onRun = { viewModel.runRoutine(routine) },
                        onToggle = { viewModel.toggleRoutine(routine) },
                        onDelete = { viewModel.deleteRoutine(routine) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRoutineDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, trigger, desc, stepsJson ->
                viewModel.saveRoutine(title, trigger, desc, stepsJson)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun RoutineCard(
    routine: RoutineEntity,
    onRun: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderCyan, RoundedCornerShape(16.dp))
            .testTag("routine_card_${routine.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(JarvisCyanPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (routine.iconName) {
                                "school" -> Icons.Default.School
                                "wb_sunny" -> Icons.Default.WbSunny
                                "fitness_center" -> Icons.Default.FitnessCenter
                                "bedtime" -> Icons.Default.Bedtime
                                else -> Icons.Default.Bolt
                            },
                            contentDescription = null,
                            tint = JarvisCyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = routine.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Say: \"${routine.triggerPhrase}\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisCyanLight
                        )
                    }
                }

                Switch(
                    checked = routine.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisCyanPrimary,
                        checkedTrackColor = JarvisSurfaceVariantDark
                    ),
                    modifier = Modifier.testTag("routine_toggle_${routine.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = routine.description,
                style = MaterialTheme.typography.bodySmall,
                color = JarvisTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete routine",
                        tint = JarvisTextMuted
                    )
                }

                Button(
                    onClick = onRun,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyanPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("run_routine_button_${routine.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Execute Routine", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreateRoutineDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf("Focus Routine") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Custom Routine", color = JarvisCyanLight, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Name") },
                    placeholder = { Text("e.g. Focus Routine") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Voice Trigger Phrase") },
                    placeholder = { Text("e.g. focus mode lagao") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Mute phone and start 25m timer") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && trigger.isNotBlank()) {
                        val steps = """[{"actionType":"VOLUME_CONTROL","parameters":{"mode":"silent"}},{"actionType":"SET_TIMER","parameters":{"seconds":"1500","message":"$title"}}]"""
                        onSave(title, trigger, description.ifBlank { "Custom routine $title" }, steps)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCyanPrimary,
                    contentColor = Color.Black
                )
            ) {
                Text("Save Routine")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = JarvisTextSecondary)
            }
        },
        containerColor = JarvisSurfaceDark
    )
}
