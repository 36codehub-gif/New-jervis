package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SecurityConfirmationDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.JarvisCoreScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.JarvisViewModel

enum class NavigationScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CORE("Jarvis", Icons.Default.GraphicEq),
    ROUTINES("Routines", Icons.Default.Bolt),
    PERMISSIONS("Services", Icons.Default.Security),
    HISTORY("History", Icons.Default.History)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JarvisApp()
            }
        }
    }
}

@Composable
fun JarvisApp(viewModel: JarvisViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(NavigationScreen.CORE) }
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()

    // Runtime Permission Launcher for standard permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisBackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = JarvisSurfaceDark,
                contentColor = JarvisCyanPrimary,
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) JarvisCyanPrimary else JarvisTextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                color = if (isSelected) JarvisCyanPrimary else JarvisTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = JarvisSurfaceVariantDark
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            when (currentScreen) {
                NavigationScreen.CORE -> JarvisCoreScreen(viewModel = viewModel)
                NavigationScreen.ROUTINES -> RoutinesScreen(viewModel = viewModel)
                NavigationScreen.PERMISSIONS -> PermissionsScreen(viewModel = viewModel)
                NavigationScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
            }

            // Security Confirmation Modal
            pendingConfirmation?.let { plan ->
                SecurityConfirmationDialog(
                    plan = plan,
                    onConfirm = { viewModel.confirmPendingAction() },
                    onCancel = { viewModel.cancelPendingAction() }
                )
            }
        }
    }
}
