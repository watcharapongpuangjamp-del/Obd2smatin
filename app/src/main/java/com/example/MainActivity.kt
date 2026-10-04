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
import com.example.ui.ObdApp
import com.example.ui.theme.ObdSmartUsbTheme
import com.example.viewmodel.ObdViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ObdViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ObdSmartUsbTheme {
                ObdApp(viewModel = viewModel)
            }
        }
        
        checkUsbIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        checkUsbIntent(intent)
    }

    private fun checkUsbIntent(intent: android.content.Intent?) {
        if (android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED == intent?.action) {
            val device = viewModel.scanForDevice()
            if (device != null) {
                viewModel.requestPermission(device)
                viewModel.connect(device)
            }
        }
    }
}
