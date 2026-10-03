package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.screens.AnalyzeScreen
import com.example.ui.screens.SimulatorScreen
import com.example.viewmodel.SlotViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Analyze : Screen("analyze", "Analyze", Icons.Default.Analytics)
    object Simulator : Screen("simulator", "Simulator", Icons.Default.Casino)
}

@Composable
fun SlotApp(viewModel: SlotViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Analyze) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(Screen.Analyze, Screen.Simulator)
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
            is Screen.Analyze -> {
                AnalyzeScreen(
                    urlInput = uiState.urlInput,
                    onUrlChange = { viewModel.onUrlChange(it) },
                    isLoading = uiState.isLoading,
                    onRunClick = { viewModel.runAnalysis() },
                    analysisResult = uiState.analysisResult,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is Screen.Simulator -> {
                SimulatorScreen(
                    credits = uiState.credits,
                    bet = uiState.bet,
                    rtp = uiState.rtpSetting,
                    onRtpChange = { viewModel.onRtpChange(it) },
                    lastResult = uiState.lastSpinResult,
                    stats = uiState.stats,
                    history = uiState.history,
                    onSpin = { viewModel.spin() },
                    onReset = { viewModel.reset() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
