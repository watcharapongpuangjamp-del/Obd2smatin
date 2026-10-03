package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.VillageEntity
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageDashboardScreen(
    villages: List<VillageEntity>,
    onVillageClick: (VillageEntity) -> Unit,
    onAddVillage: (id: String, name: String, desc: String) -> Unit,
    onSyncClick: () -> Unit,
    onAiConsultClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "พื้นที่ อสม.",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onAiConsultClick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = SurfaceDark)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.AutoAwesome, contentDescription = "AI Consult", tint = CyanPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSyncClick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = SurfaceDark)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Sync, contentDescription = "Sync", tint = CyanPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showAddDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = CyanPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "เพิ่มหมู่บ้าน", tint = DarkBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (villages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "ยังไม่มีข้อมูลพื้นที่\nกรุณาเพิ่มพื้นที่ใหม่เพื่อเริ่มต้น",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(villages) { village ->
                    VillageCard(village = village, onClick = { onVillageClick(village) })
                }
            }
        }
    }

    if (showAddDialog) {
        var id by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("เพิ่มหมู่บ้านใหม่") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("รหัสหมู่บ้าน") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ชื่อหมู่บ้าน") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("รายละเอียด") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (id.isNotBlank() && name.isNotBlank()) {
                        onAddVillage(id, name, desc)
                        showAddDialog = false
                    }
                }) {
                    Text("บันทึก")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

@Composable
fun VillageCard(
    village: VillageEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("village_card_${village.villageId}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyanPrimary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = village.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "รหัส: ${village.villageId}", color = TextSecondary, fontSize = 12.sp)
                if (village.description.isNotBlank()) {
                    Text(text = village.description, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
