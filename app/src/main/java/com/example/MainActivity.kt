package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.CyanPrimary
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.ThaiObdApp
import com.example.ui.theme.ThaiCarOBDTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.AuthViewModel
import com.example.model.AuthState
import com.example.ui.screens.LoginScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val authState by authViewModel.authState.collectAsState()
            
            ThaiCarOBDTheme(appTheme = appTheme) {
                when (authState) {
                    is AuthState.Authenticated -> {
                        var showSplash by remember { mutableStateOf(true) }
                        var showExitDialog by remember { mutableStateOf(false) }

                        if (showSplash) {
                            com.example.ui.screens.SplashScreen(
                                logoResId = android.R.drawable.ic_menu_compass,
                                onSplashFinished = { showSplash = false }
                            )
                        } else {
                            BackHandler {
                                showExitDialog = true
                            }
                            
                            ThaiObdApp(viewModel = viewModel)

                            if (showExitDialog) {
                                ExitConfirmationDialog(
                                    onConfirm = { finish() },
                                    onDismiss = { showExitDialog = false }
                                )
                            }
                        }
                    }
                    is AuthState.Unauthenticated, is AuthState.Error -> {
                        LoginScreen(onSignInClick = {
                            // In a real app, trigger Google Sign-In intent here
                            // For now, we assume the user will be authenticated via Firebase
                        })
                    }
                    is AuthState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyanPrimary)
                        }
                    }
                    else -> {}
                }
            }
        }
        
        // Handle USB attach intent
        if (android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            viewModel.connectUsbHardware()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            viewModel.connectUsbHardware()
        }
    }
}
