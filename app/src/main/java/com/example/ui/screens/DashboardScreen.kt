package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.ObdDataState
import com.example.ui.components.Gauge
import com.example.ui.theme.DarkGray800
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Success
import com.example.ui.theme.Danger

@Composable
fun DashboardScreen(
    state: ObdDataState,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Connection Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGray800)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Connection Status", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = state.connectionStatus.name,
                        color = when(state.connectionStatus) {
                            ConnectionStatus.CONNECTED -> Success
                            ConnectionStatus.ERROR -> Danger
                            else -> Color.White
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (state.connectionStatus != ConnectionStatus.CONNECTED) {
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                    ) {
                        Icon(Icons.Default.Usb, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Gauges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Gauge(
                value = state.rpm.toFloat(),
                maxValue = 8000f,
                label = "RPM",
                unit = "rev/min"
            )
            Gauge(
                value = state.speed.toFloat(),
                maxValue = 240f,
                label = "Speed",
                unit = "km/h"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secondary Data
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                label = "Coolant",
                value = "${state.coolantTemp}°C",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                label = "Voltage",
                value = state.voltage,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                label = "Load",
                value = "${state.engineLoad.toInt()}%",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Protocol: ${state.currentProtocol}",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
