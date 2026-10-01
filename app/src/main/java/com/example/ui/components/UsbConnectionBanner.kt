package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hardware.usb.UsbConnectionUiEvent
import com.example.model.ConnectionState
import com.example.ui.theme.*

/**
 * Animated banner & status card that displays real-time USB OTG & Vehicle connection status
 * with distinct visual cues (pulsing green glow for connected, alert amber for handshake, crimson for disconnected/error).
 */
@Composable
fun UsbConnectionBanner(
    connectionState: ConnectionState,
    connectedDeviceName: String?,
    chipsetType: String?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isProgressing = connectionState.ordinal in ConnectionState.DEVICE_DETECTED.ordinal..ConnectionState.LIVE_DATA_VALIDATED.ordinal
    val isError = connectionState.isError || connectionState == ConnectionState.DISCONNECTED

    // Infinite breathing glow animation for connected / handshake states
    val infiniteTransition = rememberInfiniteTransition(label = "usb_banner_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProgressing) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val primaryColor = when {
        isConnected -> EmeraldConnected
        isProgressing -> AmberWarning
        connectionState == ConnectionState.DEVICE_DETECTED -> CyanPrimary
        else -> if (connectionState.isError) RedCritical else TextSecondary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("usb_connection_banner"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isConnected || isProgressing) 1.5.dp else 1.dp,
            color = if (isConnected) EmeraldConnected.copy(alpha = glowAlpha)
                    else if (isProgressing) AmberWarning.copy(alpha = glowAlpha)
                    else SurfaceCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing Status Dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .then(
                                if (isConnected || isProgressing) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = primaryColor.copy(alpha = glowAlpha),
                                        shape = CircleShape
                                    )
                                } else Modifier
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isConnected) "เชื่อมต่อกับรถยนต์แล้ว" else connectionState.labelTh,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isConnected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(EmeraldConnected.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE ECU",
                                        color = EmeraldConnected,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Hardware / Chipset Details
                        val detailText = when {
                            connectedDeviceName != null && chipsetType != null -> "$connectedDeviceName ($chipsetType)"
                            connectedDeviceName != null -> connectedDeviceName
                            isConnected -> "ELM327 USB Adapter @ 38400 baud"
                            else -> "รอเสียบสาย USB OTG หรือเปิดสวิตช์กุญแจรถ"
                        }

                        Text(
                            text = detailText,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Action Button
                if (isConnected || isProgressing || connectionState == ConnectionState.USB_OPEN || connectionState == ConnectionState.SERIAL_READY) {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RedCritical
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedCritical.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_banner_disconnect")
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ตัดการเชื่อมต่อ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_banner_connect")
                    ) {
                        Icon(Icons.Default.Cable, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("เชื่อมต่อ USB", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Connection Progress Indicator
            if (isProgressing) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AmberWarning,
                    trackColor = SurfaceCard
                )
            }
        }
    }
}

/**
 * Toast/Snackbar banner floating overlay for single-shot USB connection/disconnection events
 */
@Composable
fun UsbConnectionNotificationToast(
    event: UsbConnectionUiEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (event == null) return@AnimatedVisibility

        val (bgColor, icon, title, message) = when (event) {
            is UsbConnectionUiEvent.DevicePluggedIn -> {
                Quadruple(
                    CyanPrimary.copy(alpha = 0.15f),
                    Icons.Default.Usb,
                    "ตรวจพบสาย USB OTG",
                    "เชื่อมต่ออุปกรณ์ ${event.deviceName} (${event.chipType})"
                )
            }
            is UsbConnectionUiEvent.DeviceUnplugged -> {
                Quadruple(
                    RedCritical.copy(alpha = 0.15f),
                    Icons.Default.UsbOff,
                    "ถอดสาย USB แล้ว",
                    "อุปกรณ์ ${event.deviceName} ถูกตัดการเชื่อมต่อ"
                )
            }
            is UsbConnectionUiEvent.VehicleConnected -> {
                Quadruple(
                    EmeraldConnected.copy(alpha = 0.15f),
                    Icons.Default.CheckCircle,
                    "เชื่อมต่อ ECU สำเร็จ!",
                    "ระบบพร้อมอ่านค่า Live Telemetry ผ่าน ${event.protocol}"
                )
            }
            is UsbConnectionUiEvent.VehicleDisconnected -> {
                Quadruple(
                    AmberWarning.copy(alpha = 0.15f),
                    Icons.Default.Cancel,
                    "ตัดการเชื่อมต่อกับรถยนต์",
                    event.reason
                )
            }
            is UsbConnectionUiEvent.PermissionPromptRequired -> {
                Quadruple(
                    AmberWarning.copy(alpha = 0.15f),
                    Icons.Default.VpnKey,
                    "รอการอนุญาตสิทธิ์ USB",
                    "กรุณากดยินยอมสิทธิ์ USB บนหน้าจอเพื่อเชื่อมต่อกับ ${event.deviceName}"
                )
            }
            is UsbConnectionUiEvent.ConnectionError -> {
                Quadruple(
                    RedCritical.copy(alpha = 0.15f),
                    Icons.Default.Warning,
                    "การเชื่อมต่อขัดข้อง",
                    event.errorMessage
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("usb_notification_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, bgColor.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Text(text = message, color = TextSecondary, fontSize = 11.sp)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
