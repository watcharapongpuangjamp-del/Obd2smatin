package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.ObdProtocol
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticScreen
import com.example.viewmodel.ObdViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Diagnostic : Screen("diagnostic", "Diagnostic", Icons.Default.Build)
}

@Composable
fun ObdApp(viewModel: ObdViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val state by viewModel.obdState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(Screen.Dashboard, Screen.Diagnostic)
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            is Screen.Dashboard -> {
                DashboardScreen(
                    state = state,
                    onConnectClick = {
                        val device = viewModel.scanForDevice()
                        if (device != null) {
                            viewModel.requestPermission(device)
                            viewModel.connect(device)
                        }
                    },
                    onProtocolSelected = { viewModel.setSelectedProtocol(it) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is Screen.Diagnostic -> {
                DiagnosticScreen(
                    dtcs = state.dtcs,
                    onScanClick = { viewModel.scanDtcs() },
                    onClearClick = { viewModel.clearDtcs() },
                    onResetClick = { viewModel.resetAdapter() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
