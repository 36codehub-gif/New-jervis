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
import com.example.data.entity.CommandHistoryEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.JarvisViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyList.collectAsState()

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
                    text = "COMMAND AUDIT LOG",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisCyanPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "${history.size} commands executed locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary
                )
            }

            if (history.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearHistory() },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = JarvisTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No command history yet. Try speaking or typing a command!",
                    color = JarvisTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItemCard(item = item, onReExecute = {
                        viewModel.executeManualCommand(item.rawCommand)
                    })
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: CommandHistoryEntity,
    onReExecute: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()) }
    val formattedTime = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderCyan, RoundedCornerShape(14.dp))
            .testTag("history_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.intentName,
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisCyanLight,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisTextMuted,
                        fontSize = 11.sp
                    )

                    Badge(
                        containerColor = when (item.status) {
                            "SUCCESS" -> JarvisSuccessGreen
                            "FAILED" -> JarvisErrorRed
                            "PERMISSION_REQUIRED", "NEEDS_CONFIRMATION" -> JarvisAmberAccent
                            else -> JarvisCyanSecondary
                        }
                    ) {
                        Text(
                            text = item.status,
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "\"${item.rawCommand}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.resultMessage,
                style = MaterialTheme.typography.bodySmall,
                color = JarvisTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReExecute,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyanPrimary),
                    modifier = Modifier.testTag("re_execute_button_${item.id}")
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run Again", fontSize = 11.sp)
                }
            }
        }
    }
}
