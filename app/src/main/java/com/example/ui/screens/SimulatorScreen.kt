package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpinResult
import com.example.viewmodel.SlotStats
import com.example.ui.theme.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@Composable
fun SimulatorScreen(
    credits: Double,
    bet: Double,
    rtp: Double,
    onRtpChange: (Double) -> Unit,
    lastResult: SpinResult?,
    stats: SlotStats,
    history: List<Double>,
    onSpin: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Slot Demo Simulator",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Reels Display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Reel(symbol = lastResult?.reels?.getOrNull(0) ?: "❓")
            Reel(symbol = lastResult?.reels?.getOrNull(1) ?: "❓")
            Reel(symbol = lastResult?.reels?.getOrNull(2) ?: "❓")
        }

        if (lastResult != null && lastResult.win > 0) {
            Text(
                text = "WIN: ${lastResult.win.toInt()} (x${lastResult.multiplier})",
                color = Success,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Credits and Bet
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatsCard(label = "CREDITS", value = credits.toInt().toString(), color = Emerald400)
            StatsCard(label = "BET", value = bet.toInt().toString(), color = Info)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // RTP Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Adjust RTP: ${(rtp * 100).toInt()}%",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = rtp.toFloat(),
                onValueChange = { onRtpChange(it.toDouble()) },
                valueRange = 0.80f..0.99f,
                colors = SliderDefaults.colors(
                    thumbColor = Emerald500,
                    activeTrackColor = Emerald500
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSpin,
                modifier = Modifier.weight(2f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Casino, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("หมุน 🎲", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkGray700)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MiniStat(label = "Spins", value = stats.spins.toString())
            MiniStat(label = "Wins", value = stats.wins.toString())
            MiniStat(label = "Losses", value = stats.losses.toString())
            MiniStat(
                label = "Profit", 
                value = stats.totalProfit.toInt().toString(),
                color = if (stats.totalProfit >= 0) Success else Danger
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History Chart
        Text(
            text = "Credit History",
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Start)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(DarkGray800, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(history) {
                modelProducer.runTransaction {
                    lineSeries { series(history.takeLast(20)) }
                }
            }
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                ),
                modelProducer = modelProducer,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "เพื่อการศึกษา ไม่สนับสนุนการพนัน",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun Reel(symbol: String) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(DarkGray700, DarkGray900)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, fontSize = 48.sp)
    }
}

@Composable
fun StatsCard(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun MiniStat(label: String, value: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}
