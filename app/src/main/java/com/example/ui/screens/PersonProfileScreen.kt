package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import com.example.db.PersonEntity
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonProfileScreen(
    household: HouseholdEntity,
    persons: List<PersonEntity>,
    onBackClick: () -> Unit,
    onAddPerson: (title: String, firstName: String, lastName: String, birthDate: Long) -> Unit,
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
                    text = "สมาชิกในบ้าน",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "บ้านเลขที่ ${household.houseNo}",
                    color = CyanPrimary,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { showAddDialog = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = CyanPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "เพิ่มสมาชิก", tint = DarkBackground)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (persons.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "ยังไม่มีข้อมูลสมาชิก\nกรุณาเพิ่มข้อมูลใหม่",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(persons) { person ->
                    PersonCard(person = person)
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("นาย") }
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("เพิ่มสมาชิกใหม่") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("คำนำหน้า") })
                    OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("ชื่อ") })
                    OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("นามสกุล") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (firstName.isNotBlank() && lastName.isNotBlank()) {
                        onAddPerson(title, firstName, lastName, System.currentTimeMillis())
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
fun PersonCard(
    person: PersonEntity
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("person_card_${person.personUuid}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = CyanPrimary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${person.title}${person.firstName} ${person.lastName}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Identity: ${person.personUuid.take(8)}...",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "บันทึกเมื่อ: ${dateFormat.format(Date(person.lastUpdated))}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
