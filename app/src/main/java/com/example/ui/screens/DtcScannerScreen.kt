package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.GeminiShareUtil
import java.text.SimpleDateFormat
import java.util.*

enum class DtcScanTab(val titleTh: String) {
    LIVE_SCAN("สแกนสดจาก ECU"),
    CACHED_HISTORY("ประวัติการสแกน (Room)")
}

@Composable
fun GeminiProGlowingButton(
    dtcCodes: List<DtcCode>,
    vehicleInfo: VehicleInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    Button(
        onClick = {
            GeminiShareUtil.copyPromptAndOpenGemini(context, dtcCodes, vehicleInfo)
        },
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp * glowAlpha,
                shape = RoundedCornerShape(12.dp),
                ambientColor = PurpleGlow,
                spotColor = PurpleGlow
            ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(PurpleGlow, CyanPrimary)))
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("วิเคราะห์ด้วย Gemini Pro", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun DtcScannerScreen(
    dtcCodes: List<DtcCode>,
    isScanning: Boolean,
    activeMode: AppOperationMode,
    scanHistory: List<CachedDtcScanHistory> = emptyList(),
    onStartScan: () -> Unit,
    onClearDtcs: () -> Unit,
    onLoadCachedScan: (CachedDtcScanHistory) -> Unit = {},
    onDeleteCachedScan: (Long) -> Unit = {},
    onClearAllCachedScans: () -> Unit = {},
    onNavigateToAiMechanic: () -> Unit,
    onExportPdfReport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DtcScanTab.LIVE_SCAN) }
    var expandedHistoryId by remember { mutableStateOf<Long?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("ล้างประวัติการสแกนทั้งหมด?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("คุณต้องการลบประวัติรหัส DTC ทั้งหมดที่บันทึกไว้ในเครื่องใช่หรือไม่?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllCachedScans()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCritical)
                ) {
                    Text("ลบทั้งหมด", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("ยกเลิก", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .testTag("dtc_scanner_screen")
    ) {
        // Navigation Tab Selector (Live Scan vs Cached History)
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = SurfaceDark,
            contentColor = CyanPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == DtcScanTab.LIVE_SCAN,
                onClick = { selectedTab = DtcScanTab.LIVE_SCAN },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == DtcScanTab.LIVE_SCAN) CyanPrimary else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DtcScanTab.LIVE_SCAN.titleTh,
                            fontWeight = if (selectedTab == DtcScanTab.LIVE_SCAN) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == DtcScanTab.LIVE_SCAN) CyanPrimary else TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                modifier = Modifier.testTag("tab_live_scan")
            )
            Tab(
                selected = selectedTab == DtcScanTab.CACHED_HISTORY,
                onClick = { selectedTab = DtcScanTab.CACHED_HISTORY },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == DtcScanTab.CACHED_HISTORY) CyanPrimary else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DtcScanTab.CACHED_HISTORY.titleTh,
                            fontWeight = if (selectedTab == DtcScanTab.CACHED_HISTORY) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == DtcScanTab.CACHED_HISTORY) CyanPrimary else TextSecondary,
                            fontSize = 13.sp
                        )
                        if (scanHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedTab == DtcScanTab.CACHED_HISTORY) CyanPrimary else SurfaceCard)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${scanHistory.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == DtcScanTab.CACHED_HISTORY) DarkBackground else TextPrimary
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("tab_cached_history")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            DtcScanTab.LIVE_SCAN -> {
                // Live Diagnostic Section
                LiveDiagnosticScanView(
                    dtcCodes = dtcCodes,
                    isScanning = isScanning,
                    activeMode = activeMode,
                    onStartScan = onStartScan,
                    onClearDtcs = onClearDtcs,
                    onNavigateToAiMechanic = onNavigateToAiMechanic,
                    onExportPdfReport = onExportPdfReport
                )
            }
            DtcScanTab.CACHED_HISTORY -> {
                // Room Offline Cached History Section
                CachedDtcHistoryView(
                    scanHistory = scanHistory,
                    expandedHistoryId = expandedHistoryId,
                    onToggleExpand = { id ->
                        expandedHistoryId = if (expandedHistoryId == id) null else id
                    },
                    onLoadScan = { historyItem ->
                        onLoadCachedScan(historyItem)
                        selectedTab = DtcScanTab.LIVE_SCAN
                    },
                    onLoadScanToAi = { historyItem ->
                        onLoadCachedScan(historyItem)
                        onNavigateToAiMechanic()
                    },
                    onDeleteScan = onDeleteCachedScan,
                    onRequestClearAll = { showClearConfirmDialog = true },
                    onNavigateToLiveScan = { selectedTab = DtcScanTab.LIVE_SCAN }
                )
            }
        }
    }
}

@Composable
private fun LiveDiagnosticScanView(
    dtcCodes: List<DtcCode>,
    isScanning: Boolean,
    activeMode: AppOperationMode,
    onStartScan: () -> Unit,
    onClearDtcs: () -> Unit,
    onNavigateToAiMechanic: () -> Unit,
    onExportPdfReport: (() -> Unit)?
) {
    var showDtcClearConfirmDialog by remember { mutableStateOf(false) }

    if (showDtcClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDtcClearConfirmDialog = false },
            title = {
                Text(
                    text = "ลบรหัส DTC และรีเซ็ตไฟรูปเครื่อง?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "ระบบจะส่งคำสั่ง Mode 04 ไปยังกล่อง ECU เพื่อลบรหัสความผิดปกติ และจะเริ่มติดตามรอบการขับขี่ (Drive Cycle 500 กม.) พร้อมแจ้งเตือนรอบบำรุงรักษาโดยอัตโนมัติ",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearDtcs()
                        showDtcClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCritical)
                ) {
                    Text("ลบโค้ด & เริ่มจับรอบ", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDtcClearConfirmDialog = false }) {
                    Text("ยกเลิก", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Module Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ระบบวินิจฉัยรถยนต์ (Multi-Module Scan)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeMode == AppOperationMode.REAL_HARDWARE) EmeraldConnected.copy(0.2f) else PurpleAi.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (activeMode == AppOperationMode.REAL_HARDWARE) "REAL ECU SCAN" else "SIMULATOR SCAN",
                            color = if (activeMode == AppOperationMode.REAL_HARDWARE) EmeraldConnected else PurpleGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("ECM เครื่อง", "TCM เกียร์", "ABS เบรก", "SRS ถุงลม", "BCM ตัวถัง").forEach { moduleName ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCard)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = moduleName,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scan Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStartScan,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_start_dtc_scan")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("กำลังสแกน ECU...", color = DarkBackground, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan DTC", modifier = Modifier.size(18.dp), tint = DarkBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("สแกนรหัส DTC ทั้งหมด", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            if (dtcCodes.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showDtcClearConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCritical),
                    modifier = Modifier.testTag("btn_clear_dtc_codes")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear DTCs", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ลบโค้ด")
                }
            }

            if (onExportPdfReport != null) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onExportPdfReport,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceCard, contentColor = CyanPrimary),
                    modifier = Modifier.testTag("btn_export_pdf_report")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // DTC Results List
        if (dtcCodes.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No DTCs",
                        tint = EmeraldConnected,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ไม่พบรหัสความผิดปกติ (No DTC Codes Found)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ระบบกล่องควบคุม ECU เครื่องยนต์, เกียร์, ABS และระบบไฟฟ้าทำงานในเกณฑ์ปกติ ทุกการสแกนจะถูกบันทึกประวัติลงฐานข้อมูลในเครื่องอัตโนมัติ",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // DTC Codes List View
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "พบข้อผิดปกติทั้งหมด ${dtcCodes.size} รหัส:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = onNavigateToAiMechanic,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAi),
                        modifier = Modifier.testTag("btn_ask_ai_mechanic")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ส่งให้ AI Mechanic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Glowing Action Button for Gemini Pro
                GeminiProGlowingButton(
                    dtcCodes = dtcCodes,
                    vehicleInfo = VehicleInfo(brand = "Isuzu", model = "D-Max", year = 2023)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dtcCodes) { dtc ->
                        val severityColor = when (dtc.severity) {
                            DtcSeverity.CRITICAL -> RedCritical
                            DtcSeverity.WARNING -> AmberWarning
                            DtcSeverity.INFO -> CyanPrimary
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dtc_item_${dtc.code}"),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(severityColor.copy(0.2f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = dtc.code,
                                                color = severityColor,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = dtc.module,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SurfaceCard)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = dtc.modeProvenance.name,
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = dtc.descriptionTh,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dtc.descriptionEn,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CachedDtcHistoryView(
    scanHistory: List<CachedDtcScanHistory>,
    expandedHistoryId: Long?,
    onToggleExpand: (Long) -> Unit,
    onLoadScan: (CachedDtcScanHistory) -> Unit,
    onLoadScanToAi: (CachedDtcScanHistory) -> Unit,
    onDeleteScan: (Long) -> Unit,
    onRequestClearAll: () -> Unit,
    onNavigateToLiveScan: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm น.", Locale.forLanguageTag("th-TH")) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Offline Cache Info Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanPrimary.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "คลังประวัติรหัสข้อผิดพลาด (Room Offline Cache)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "บันทึกในเครื่องอัตโนมัติ เปิดดูและส่งให้ AI วิเคราะห์ได้แม้ดับเครื่องหรือถอดสาย USB",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History Actions Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ประวัติการสแกนทั้งหมด (${scanHistory.size} ครั้ง)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            if (scanHistory.isNotEmpty()) {
                TextButton(
                    onClick = onRequestClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = RedCritical)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ล้างประวัติ", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (scanHistory.isEmpty()) {
            // Empty History State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryEdu,
                        contentDescription = "Empty History",
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ยังไม่มีประวัติการสแกนที่บันทึกไว้",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "เมื่อคุณเริ่มกด 'สแกนรหัส DTC ทั้งหมด' ข้อมูลโค้ดของรถยนต์จะถูกบันทึกประวัติลงฐานข้อมูล Room ทันที",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToLiveScan,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("เริ่มสแกน ECU", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Historical Records List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scanHistory, key = { it.id }) { historyItem ->
                    val isExpanded = expandedHistoryId == historyItem.id
                    val formattedDate = try {
                        dateFormat.format(Date(historyItem.timestamp))
                    } catch (e: Exception) {
                        "วันที่ไม่ระบุ"
                    }

                    val hasCodes = historyItem.codes.isNotEmpty()
                    val badgeColor = if (hasCodes) RedCritical else EmeraldConnected

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cached_scan_record_${historyItem.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isExpanded) BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleExpand(historyItem.id) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(badgeColor.copy(0.18f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (hasCodes) "พบ ${historyItem.codes.size} รหัสข้อผิดพลาด" else "ระบบปกติ (0 โค้ด)",
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SurfaceCard)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (historyItem.modeProvenance.contains("REAL")) "HARDWARE" else "SIMULATOR",
                                                color = TextMuted,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = historyItem.vehicleName,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Text(
                                        text = formattedDate,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(onClick = { onToggleExpand(historyItem.id) }) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand details",
                                        tint = CyanPrimary
                                    )
                                }
                            }

                            // Expanded DTC details
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    HorizontalDivider(color = SurfaceCard, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (historyItem.codes.isEmpty()) {
                                        Text(
                                            text = "การสแกนในครั้งนี้ไม่พบรหัสความผิดปกติของกล่องควบคุม ECU ใดๆ",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Text(
                                            text = "รายการรหัส DTC ที่บันทึกไว้ในแคช:",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        historyItem.codes.forEach { code ->
                                            val sevColor = when (code.severity) {
                                                DtcSeverity.CRITICAL -> RedCritical
                                                DtcSeverity.WARNING -> AmberWarning
                                                DtcSeverity.INFO -> CyanPrimary
                                            }

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = code.code,
                                                                color = sevColor,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 14.sp
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "โมดูล: ${code.module}",
                                                                color = TextSecondary,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                        Text(
                                                            text = code.severity.name,
                                                            color = sevColor,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = code.descriptionTh,
                                                        color = TextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = code.descriptionEn,
                                                        color = TextMuted,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Action buttons for this cached scan
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (hasCodes) {
                                            Button(
                                                onClick = { onLoadScanToAi(historyItem) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAi),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("วิเคราะห์ด้วย AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    GeminiShareUtil.copyPromptAndOpenGemini(
                                                        context,
                                                        historyItem.codes,
                                                        VehicleInfo("Isuzu", "D-Max", 2023)
                                                    )
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                                                border = BorderStroke(1.dp, CyanPrimary)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Gemini", fontSize = 11.sp)
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteScan(historyItem.id) }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete record", tint = RedCritical)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
