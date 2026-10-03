package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppTheme
import com.example.db.VehicleProfileEntity
import com.example.ui.theme.*

@Composable
fun VehicleProfileScreen(
    profiles: List<VehicleProfileEntity>,
    selectedProfile: VehicleProfileEntity?,
    appTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
    onSelectProfile: (VehicleProfileEntity) -> Unit,
    onAddProfile: (name: String, make: String, model: String, year: Int, engine: String, plate: String, mileage: Int) -> Unit,
    onAddMaintenanceLog: (title: String, cost: Double, mileage: Int, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddLogDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("vehicle_profile_screen")
    ) {
        // Vehicle Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "โปรไฟล์รถยนต์ (Vehicle Profile)",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Button(
                        onClick = { showAddVehicleDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("btn_add_new_vehicle")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("เพิ่มรถยนต์", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedProfile != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = selectedProfile.name,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${selectedProfile.make} ${selectedProfile.model} (ปี ${selectedProfile.year})",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "เครื่องยนต์: ${selectedProfile.engineType} | ทะเบียน: ${selectedProfile.licensePlate}",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "เลขกิโลเมตรสะสม: ${selectedProfile.odometerKm} กม.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle List Selectors
        Text(
            text = "รายการรถยนต์ที่ลงทะเบียนในระบบ Room DB (${profiles.size} คัน):",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        profiles.forEach { profile ->
            val isSelected = profile.id == selectedProfile?.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectProfile(profile) }
                    .testTag("vehicle_card_${profile.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = profile.name,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${profile.make} ${profile.model} - ${profile.licensePlate}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("กำลังเลือก", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Maintenance Action Button
        Button(
            onClick = { showAddLogDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_add_maintenance_log")
        ) {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("บันทึกประวัติการซ่อมบำรุง / เข้าศูนย์", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Theme Switcher Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ธีมและการแสดงผล (Theme & UI)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOption(
                        title = "โหมดสว่าง",
                        isSelected = appTheme == AppTheme.LIGHT,
                        onClick = { onThemeChanged(AppTheme.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        title = "โหมดมืด",
                        isSelected = appTheme == AppTheme.DARK,
                        onClick = { onThemeChanged(AppTheme.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ThemeOption(
                    title = "Night Driving (High Contrast)",
                    subtitle = "มืดสนิท คอนทราสต์สูง ลดแสงสะท้อนขณะขับขี่กลางคืน",
                    isSelected = appTheme == AppTheme.NIGHT_DRIVING,
                    onClick = { onThemeChanged(AppTheme.NIGHT_DRIVING) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        var name by remember { mutableStateOf("") }
        var make by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("2024") }
        var engine by remember { mutableStateOf("") }
        var plate by remember { mutableStateOf("") }
        var odometer by remember { mutableStateOf("25000") }

        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("เพิ่มรถยนต์คันใหม่ (Room DB)", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ชื่อเรียกคันนี้") })
                    OutlinedTextField(value = make, onValueChange = { make = it }, label = { Text("ยี่ห้อ (เช่น Toyota, Honda)") })
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("รุ่น (เช่น Civic, Hilux)") })
                    OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("ปี ค.ศ.") })
                    OutlinedTextField(value = engine, onValueChange = { engine = it }, label = { Text("ขนาด/รหัสเครื่องยนต์") })
                    OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("ทะเบียนรถ") })
                    OutlinedTextField(value = odometer, onValueChange = { odometer = it }, label = { Text("เลขไมล์ปัจจุบัน (กม.)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && make.isNotBlank()) {
                            onAddProfile(
                                name,
                                make,
                                model,
                                year.toIntOrNull() ?: 2024,
                                engine,
                                plate,
                                odometer.toIntOrNull() ?: 0
                            )
                            showAddVehicleDialog = false
                        }
                    }
                ) {
                    Text("บันทึก")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    // Add Maintenance Log Dialog
    if (showAddLogDialog) {
        var title by remember { mutableStateOf("เปลี่ยนถ่ายน้ำมันเครื่องสังเคราะห์") }
        var cost by remember { mutableStateOf("2400") }
        var mileage by remember { mutableStateOf("48000") }
        var category by remember { mutableStateOf("Oil Change") }

        AlertDialog(
            onDismissRequest = { showAddLogDialog = false },
            title = { Text("บันทึกการซ่อมบำรุง (Maintenance Log)", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("รายการซ่อม/เช็กระยะ") })
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("ค่าใช้จ่าย (บาท)") })
                    OutlinedTextField(value = mileage, onValueChange = { mileage = it }, label = { Text("เลขไมล์ตอนเปลี่ยน (กม.)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddMaintenanceLog(
                                title,
                                cost.toDoubleOrNull() ?: 0.0,
                                mileage.toIntOrNull() ?: 0,
                                category
                            )
                            showAddLogDialog = false
                        }
                    }
                ) {
                    Text("บันทึก")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLogDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("theme_option_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
