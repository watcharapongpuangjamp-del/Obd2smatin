package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AnalysisResult
import com.example.ui.theme.*

@Composable
fun AnalyzeScreen(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    isLoading: Boolean,
    onRunClick: () -> Unit,
    analysisResult: AnalysisResult?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1791047179509),
                contentDescription = "Hero Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Slot Insight Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Analyze URLs & Simulate Gameplay",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Emerald400
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input Section
        Text(
            text = "วาง URL เกมสล็อตเพื่อวิเคราะห์",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://casino.com/game-slug", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = Emerald700,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Emerald500
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRunClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Success),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && urlInput.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("▶ Run Analysis", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Results Section
        if (analysisResult != null) {
            AnalysisResultCards(result = analysisResult)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Disclaimer
        Text(
            text = "⚠️ Disclaimer: เพื่อการศึกษา ไม่สนับสนุนการพนัน",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun AnalysisResultCards(result: AnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Casino Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGray800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Casino Info", color = Emerald400, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when {
                        result.casino?.isBlacklisted == true -> Danger
                        result.casino?.trustScore ?: 0 >= 80 -> Success
                        result.casino?.trustScore == -1 -> Color.Gray
                        else -> Warning
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.casino?.status ?: "Unknown",
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text("Domain: ${result.casino?.domain}", color = Color.LightGray, fontSize = 14.sp)
                if (result.casino?.trustScore ?: -1 != -1) {
                    Text("Trust Score: ${result.casino?.trustScore}/100", color = Color.White)
                }
                if (result.casino?.reason?.isNotEmpty() == true) {
                    Text("Reason: ${result.casino.reason}", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        // Game Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGray800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Game Info", color = Emerald400, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = result.game?.name ?: "Unknown Game",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem(label = "RTP", value = "${(result.game?.rtp ?: 0.96) * 100}%")
                    InfoItem(label = "Volatility", value = result.game?.volatility ?: "Medium")
                    InfoItem(label = "Provider", value = result.game?.provider ?: "Unknown")
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
