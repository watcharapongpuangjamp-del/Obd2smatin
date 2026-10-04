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
import com.example.model.ObdProtocol
import com.example.ui.components.Gauge
import com.example.ui.theme.DarkGray800
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Success
import com.example.ui.theme.Danger

@Composable
fun DashboardScreen(
    state: ObdDataState,
    onConnectClick: () -> Unit,
    onProtocolSelected: (ObdProtocol) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProtocolMenu by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Connection Status", color = Color.Gray, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when(state.connectionStatus) {
                                            ConnectionStatus.CONNECTED -> Success
                                            ConnectionStatus.ERROR -> Danger
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.connectionStatus.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Battery Voltage Chip
                    if (state.connectionStatus == ConnectionStatus.CONNECTED) {
                        Surface(
                            color = Emerald500.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500)
                        ) {
                            Text(
                                text = state.voltage,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = Emerald500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    if (state.connectionStatus != ConnectionStatus.CONNECTED && state.connectionStatus != ConnectionStatus.CONNECTING) {
                        Button(
                            onClick = onConnectClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect")
                        }
                    } else if (state.connectionStatus == ConnectionStatus.CONNECTING) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Emerald500)
                    }
                }

                if (state.connectionStatus != ConnectionStatus.CONNECTED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Protocol Setting", color = Color.Gray, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showProtocolMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(state.selectedProtocol.description, color = Color.White)
                        }
                        
                        DropdownMenu(
                            expanded = showProtocolMenu,
                            onDismissRequest = { showProtocolMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f).background(DarkGray800)
                        ) {
                            com.example.model.ObdProtocol.values().forEach { protocol ->
                                DropdownMenuItem(
                                    text = { Text(protocol.description, color = Color.White) },
                                    onClick = {
                                        onProtocolSelected(protocol)
                                        showProtocolMenu = false
                                    }
                                )
                            }
                        }
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
