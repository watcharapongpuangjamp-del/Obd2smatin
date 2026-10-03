package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.HouseholdEntity
import com.example.db.VillageEntity
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdRegistryScreen(
    village: VillageEntity,
    households: List<HouseholdEntity>,
    onBackClick: () -> Unit,
    onHouseholdClick: (HouseholdEntity) -> Unit,
    onAddHousehold: (houseNo: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "ย้อนกลับ", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "รายการหลังคาเรือน",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = village.name,
                    color = CyanPrimary,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { showAddDialog = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = CyanPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "เพิ่มบ้าน", tint = DarkBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { /* Trigger file picker */ },
                colors = IconButtonDefaults.iconButtonColors(containerColor = SurfaceDark)
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.UploadFile, contentDescription = "Import", tint = CyanPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (households.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "ยังไม่มีข้อมูลหลังคาเรือน\nกรุณาเพิ่มข้อมูลใหม่",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(households) { household ->
                    HouseholdCard(household = household, onClick = { onHouseholdClick(household) })
                }
            }
        }
    }

    if (showAddDialog) {
        var houseNo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("เพิ่มหลังคาเรือนใหม่") },
            text = {
                OutlinedTextField(value = houseNo, onValueChange = { houseNo = it }, label = { Text("เลขที่บ้าน") })
            },
            confirmButton = {
                Button(onClick = {
                    if (houseNo.isNotBlank()) {
                        onAddHousehold(houseNo)
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
fun HouseholdCard(
    household: HouseholdEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("household_card_${household.householdUuid}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = CyanPrimary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "บ้านเลขที่ ${household.houseNo}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Identity: ${household.householdUuid.take(8)}...", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
