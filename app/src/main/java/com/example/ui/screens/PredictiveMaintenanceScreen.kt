package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun PredictiveMaintenanceScreen(
    items: List<PredictiveMaintenanceItem>,
    serviceIntervals: List<ServiceIntervalUiModel> = emptyList(),
    dtcClearTracker: DtcClearTrackerSummary? = null,
    onMarkServiceCompleted: (ServiceIntervalUiModel, Double, String) -> Unit = { _, _, _ -> },
    onAddCustomInterval: (String, String, ServiceCategory, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onUpdateIntervalTarget: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onDeleteInterval: (Long) -> Unit = {},
    onSimulateDrivingKm: (Int) -> Unit = {},
    onTriggerTestNotification: (ServiceIntervalUiModel) -> Unit = {},
    onTriggerDtcClearNotification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var intervalToEdit by remember { mutableStateOf<ServiceIntervalUiModel?>(null) }
    var intervalToComplete by remember { mutableStateOf<ServiceIntervalUiModel?>(null) }

    val overdueCount = serviceIntervals.count { it.urgencyStatus == ServiceUrgencyStatus.OVERDUE }
    val dueSoonCount = serviceIntervals.count { it.urgencyStatus == ServiceUrgencyStatus.DUE_SOON }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .testTag("predictive_maintenance_screen")
    ) {
        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = CyanPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "รอบเช็กระยะ & ลบโค้ด",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                        if (overdueCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(RedCritical)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$overdueCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("tab_service_intervals")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "คาดการณ์สุขภาพ (RUL)",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_predictive_rul")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // Service Intervals & DTC Clear Tracker Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Post-DTC Clear Verification Card
                item {
                    DtcClearTrackerCard(
                        tracker = dtcClearTracker,
                        onSimulateKm = { onSimulateDrivingKm(50) },
                        onTriggerAlert = onTriggerDtcClearNotification
                    )
                }

                // 2. Urgent Service Alert Banner if any items are due
                if (overdueCount > 0 || dueSoonCount > 0) {
                    item {
                        UrgentServiceBanner(
                            overdueCount = overdueCount,
                            dueSoonCount = dueSoonCount
                        )
                    }
                }

                // 3. Header with Add Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "รายการเช็กระยะตามรอบ (Maintenance Intervals)",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "แจ้งเตือนอัตโนมัติเมื่อถึงระยะทางหรือเวลาที่กำหนด",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_add_interval")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "เพิ่มรายการ",
                                tint = DarkBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "เพิ่มรอบ",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 4. List of Service Intervals
                items(serviceIntervals) { interval ->
                    ServiceIntervalCard(
                        interval = interval,
                        onMarkCompleted = { intervalToComplete = interval },
                        onEditTarget = { intervalToEdit = interval },
                        onDelete = { onDeleteInterval(interval.id) },
                        onTestNotification = { onTriggerTestNotification(interval) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        } else {
            // Predictive RUL Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "การประเมินอายุการใช้งานคงเหลือ (RUL)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ประเมินสุขภาพและระยะทางที่ยังขับขี่ได้ปลอดภัยจากพารามิเตอร์เซนเซอร์ OBD-II สด",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                items(items) { item ->
                    val healthColor = when (item.statusLevel) {
                        "GOOD" -> EmeraldConnected
                        "ATTENTION" -> AmberWarning
                        else -> RedCritical
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rul_item_${item.componentNameEn}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.componentNameTh,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SurfaceCard)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.provenance,
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "สุขภาพอะไหล่: ${item.healthPercentage}%",
                                    color = healthColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "ระยะทางคงเหลือ ~${item.rulKilometers} กม.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { (item.healthPercentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = healthColor,
                                trackColor = SurfaceCard
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.recommendationTh,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Custom Interval
    if (showAddDialog) {
        AddIntervalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { titleTh, titleEn, cat, km, days ->
                onAddCustomInterval(titleTh, titleEn, cat, km, days)
                showAddDialog = false
            }
        )
    }

    // Dialog: Edit Target Interval
    intervalToEdit?.let { interval ->
        EditIntervalTargetDialog(
            interval = interval,
            onDismiss = { intervalToEdit = null },
            onConfirm = { km, days ->
                onUpdateIntervalTarget(interval.id, km, days)
                intervalToEdit = null
            }
        )
    }

    // Dialog: Complete Service
    intervalToComplete?.let { interval ->
        CompleteServiceDialog(
            interval = interval,
            onDismiss = { intervalToComplete = null },
            onConfirm = { cost, notes ->
                onMarkServiceCompleted(interval, cost, notes)
                intervalToComplete = null
            }
        )
    }
}

@Composable
fun DtcClearTrackerCard(
    tracker: DtcClearTrackerSummary?,
    onSimulateKm: () -> Unit,
    onTriggerAlert: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .testTag("dtc_clear_tracker_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "การติดตามหลังลบโค้ด DTC (Drive Cycle)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "รอบตรวจสอบความพร้อมของ ECU และมอนิเตอร์",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                if (tracker?.hasHistory == true) {
                    val statusText = if (tracker.ecuReadinessDriveCycleCompleted) "พร้อมสมบูรณ์" else "กำลังตรวจสอบ"
                    val statusColor = if (tracker.ecuReadinessDriveCycleCompleted) EmeraldConnected else AmberWarning
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tracker == null || !tracker.hasHistory) {
                Text(
                    text = "ยังไม่มีประวัติการลบโค้ด DTC ในระบบ เมื่อคุณกดลบโค้ดในหน้าสแกน ระบบจะเริ่มจับระยะทางและเวลาสำหรับรอบการขับขี่ทดสอบอัตโนมัติ",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ระยะทางตั้งแต่ลบโค้ด",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${tracker.distanceSinceClearKm} / ${tracker.driveCycleTargetKm} กม.",
                            color = CyanPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "เวลาที่ผ่านไป",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        val timeStr = if (tracker.timeSinceClearDays > 0) {
                            "${tracker.timeSinceClearDays} วัน ${tracker.timeSinceClearHours} ชม."
                        } else {
                            "${tracker.timeSinceClearHours} ชม."
                        }
                        Text(
                            text = timeStr,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (tracker.driveCycleProgressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (tracker.ecuReadinessDriveCycleCompleted) EmeraldConnected else CyanPrimary,
                    trackColor = SurfaceCard
                )

                if (tracker.clearedCodesList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "โค้ดที่เคยลบ: ",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        tracker.clearedCodesList.take(4).forEach { code ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(RedCritical.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = code,
                                    color = RedCritical,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateKm,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_simulate_km"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CyanPrimary.copy(alpha = 0.6f)))
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+50 กม. (จำลองวิ่ง)",
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onTriggerAlert,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_test_dtc_notif"),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ส่งแจ้งเตือนเตือนซ้ำ",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun UrgentServiceBanner(
    overdueCount: Int,
    dueSoonCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (overdueCount > 0) RedCritical else AmberWarning, RoundedCornerShape(14.dp))
            .testTag("urgent_service_banner"),
        colors = CardDefaults.cardColors(
            containerColor = if (overdueCount > 0) RedCritical.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (overdueCount > 0) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = if (overdueCount > 0) RedCritical else AmberWarning,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (overdueCount > 0) "⚠️ มี $overdueCount รายการเกินกำหนดเช็กระยะแล้ว!" else "🔔 มี $dueSoonCount รายการใกล้ถึงกำหนดเช็กระยะ",
                    color = if (overdueCount > 0) RedCritical else AmberWarning,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "กรุณาเปลี่ยนถ่ายอะไหล่หรือนำรถเข้ารับบริการ เพื่อป้องกันความเสียหายของเครื่องยนต์",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun ServiceIntervalCard(
    interval: ServiceIntervalUiModel,
    onMarkCompleted: () -> Unit,
    onEditTarget: () -> Unit,
    onDelete: () -> Unit,
    onTestNotification: () -> Unit
) {
    val statusColor = when (interval.urgencyStatus) {
        ServiceUrgencyStatus.OVERDUE -> RedCritical
        ServiceUrgencyStatus.DUE_SOON -> AmberWarning
        ServiceUrgencyStatus.NORMAL -> EmeraldConnected
    }

    val iconVector: ImageVector = when (interval.category) {
        ServiceCategory.ENGINE -> Icons.Default.OilBarrel
        ServiceCategory.BRAKE -> Icons.Default.Speed
        ServiceCategory.TRANSMISSION -> Icons.Default.Settings
        ServiceCategory.FILTERS -> Icons.Default.FilterAlt
        ServiceCategory.ECU_DTC_CYCLE -> Icons.Default.DirectionsCar
        ServiceCategory.GENERAL -> Icons.Default.Build
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_interval_card_${interval.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = interval.titleTh,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = interval.titleEn,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Urgency Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = interval.urgencyStatus.labelTh,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mileage & Days detail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "รอบกำหนด",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "ทุก ${interval.targetIntervalKm} กม. / ${interval.targetIntervalDays} วัน",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ขับมาแล้ว",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${interval.distanceSinceLastServiceKm} กม. (${interval.daysSinceLastService} วัน)",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { interval.progressRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = SurfaceCard
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status Description
            Text(
                text = interval.statusDescriptionTh,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onMarkCompleted,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldConnected),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(32.dp)
                        .testTag("btn_service_done_${interval.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "บันทึกเปลี่ยนแล้ว",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onTestNotification,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .testTag("btn_notif_test_${interval.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "ทดสอบแจ้งเตือน",
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onEditTarget,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .testTag("btn_edit_target_${interval.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "ปรับรอบ",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (interval.isCustom) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard)
                            .testTag("btn_delete_${interval.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "ลบรายการ",
                            tint = RedCritical,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddIntervalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, ServiceCategory, Int, Int) -> Unit
) {
    var titleTh by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ServiceCategory.GENERAL) }
    var targetKmStr by remember { mutableStateOf("10000") }
    var targetDaysStr by remember { mutableStateOf("180") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "เพิ่มรายการเช็กระยะใหม่",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titleTh,
                    onValueChange = { titleTh = it },
                    label = { Text("ชื่อรายการ (ภาษาไทย)") },
                    placeholder = { Text("เช่น สลับยางและถ่วงล้อ 4 ล้อ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("ชื่อภาษาอังกฤษ (ถ้ามี)") },
                    placeholder = { Text("Tire Rotation & Wheel Balance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetKmStr,
                        onValueChange = { targetKmStr = it },
                        label = { Text("รอบระยะ (กม.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = targetDaysStr,
                        onValueChange = { targetDaysStr = it },
                        label = { Text("รอบเวลา (วัน)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleTh.isNotBlank()) {
                        val km = targetKmStr.toIntOrNull() ?: 10000
                        val days = targetDaysStr.toIntOrNull() ?: 180
                        val en = if (titleEn.isBlank()) titleTh else titleEn
                        onConfirm(titleTh, en, selectedCategory, km, days)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("บันทึก", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun EditIntervalTargetDialog(
    interval: ServiceIntervalUiModel,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var targetKmStr by remember { mutableStateOf(interval.targetIntervalKm.toString()) }
    var targetDaysStr by remember { mutableStateOf(interval.targetIntervalDays.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ปรับรอบกำหนด: ${interval.titleTh}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ปรับตั้งค่าระยะทางและจำนวนวันที่ต้องการให้ระบบแจ้งเตือน",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = targetKmStr,
                    onValueChange = { targetKmStr = it },
                    label = { Text("รอบระยะทาง (กิโลเมตร)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetDaysStr,
                    onValueChange = { targetDaysStr = it },
                    label = { Text("รอบเวลา (วัน)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val km = targetKmStr.toIntOrNull() ?: interval.targetIntervalKm
                    val days = targetDaysStr.toIntOrNull() ?: interval.targetIntervalDays
                    onConfirm(km, days)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("บันทึก", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun CompleteServiceDialog(
    interval: ServiceIntervalUiModel,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var costStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "บันทึกการเปลี่ยน/เช็กระยะ: ${interval.titleTh}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "การบันทึกจะรีเซ็ตการนับกิโลเมตรและเวลาของรอบนี้ และบันทึกลงในประวัติการบำรุงรักษาของรถ",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("ค่าใช้จ่าย (บาท) (ไม่ระบุก็ได้)") },
                    placeholder = { Text("เช่น 1850") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("บันทึกเพิ่มเติม / แบรนด์อะไหล่") },
                    placeholder = { Text("เช่น เปลี่ยนน้ำมันเครื่องเกรด 5W-30 พร้อมกรองแท้") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    onConfirm(cost, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldConnected)
            ) {
                Text("ยืนยันบันทึก", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}
