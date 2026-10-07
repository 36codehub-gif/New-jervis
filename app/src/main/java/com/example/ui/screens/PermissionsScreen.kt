package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.permission.PermissionItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun PermissionsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissions by viewModel.permissions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

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
                    text = "PERMISSION & SERVICE CENTER",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisCyanPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Android Jarvis never abuses or conceals permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.refreshPermissions() },
                modifier = Modifier.testTag("refresh_permissions_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = JarvisCyanLight)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(permissions, key = { it.id }) { item ->
                PermissionItemCard(item = item, context = context)
            }
        }
    }
}

@Composable
fun PermissionItemCard(item: PermissionItem, context: Context) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (item.isGranted) JarvisSuccessGreen.copy(alpha = 0.4f) else JarvisAmberAccent.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .testTag("perm_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (item.isGranted) JarvisSuccessGreen.copy(alpha = 0.15f) else JarvisAmberAccent.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (item.isGranted) JarvisSuccessGreen else JarvisAmberAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = JarvisTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Badge(
                        containerColor = if (item.isGranted) JarvisSuccessGreen else JarvisAmberAccent
                    ) {
                        Text(
                            text = if (item.isGranted) "GRANTED" else "SETUP NEEDED",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            if (!item.isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(item.settingsIntent)
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisAmberAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("enable_perm_button_${item.id}")
                ) {
                    Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
