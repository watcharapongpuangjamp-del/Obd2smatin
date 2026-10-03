package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hardware.usb.UsbConnectionUiEvent
import com.example.ui.components.UsbConnectionNotificationToast
import com.example.ui.screens.*
import com.example.ui.screens.developer.DeveloperStoryScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.OsmViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val titleTh: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "หน้าวัดค่า", Icons.Default.Speed)
    object DtcScan : Screen("dtc_scan", "สแกนโค้ด", Icons.Default.QrCodeScanner)
    object AiMechanic : Screen("ai_mechanic", "AI ช่างยนต์", Icons.Default.AutoAwesome)
    object Predictive : Screen("predictive", "คาดการณ์", Icons.Default.HourglassTop)
    object Profile : Screen("profile", "ข้อมูลรถ", Icons.Default.DirectionsCar)
    object DeveloperStory : Screen("developer_story", "เรื่องราวนักพัฒนา", Icons.Default.Info)
    object VillageDashboard : Screen("village_dashboard", "พื้นที่ อสม.", Icons.Default.Map)
    object HouseholdRegistry : Screen("household_registry", "หลังคาเรือน", Icons.Default.Home)
    object PersonProfile : Screen("person_profile", "สมาชิก", Icons.Default.People)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThaiObdApp(viewModel: MainViewModel) {
    val osmViewModel: OsmViewModel = viewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val telemetry by viewModel.liveTelemetry.collectAsStateWithLifecycle()
    val dtcCodes by viewModel.dtcCodes.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val isAiAnalyzing by viewModel.isAiAnalyzing.collectAsStateWithLifecycle()
    val profiles by viewModel.vehicleProfiles.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val currentScenario by viewModel.currentScenario.collectAsStateWithLifecycle()
    val connectedUsbDevice by viewModel.connectedUsbDevice.collectAsStateWithLifecycle()
    val detectedChipset by viewModel.detectedChipset.collectAsStateWithLifecycle()
    val scanHistory by viewModel.cachedScanHistory.collectAsStateWithLifecycle()
    val tripSummary by viewModel.currentTripSummary.collectAsStateWithLifecycle(initialValue = null)
    val tripStatus by viewModel.activeTripStatus.collectAsStateWithLifecycle(initialValue = com.example.model.TripStatus.PAUSED)
    val diagnosticReport by viewModel.diagnosticRuleReport.collectAsStateWithLifecycle()
    val serviceIntervals by viewModel.serviceIntervals.collectAsStateWithLifecycle()
    val dtcClearTracker by viewModel.dtcClearTracker.collectAsStateWithLifecycle()
    val activeMaintenanceAlerts by viewModel.activeMaintenanceAlerts.collectAsStateWithLifecycle()

    // OSM States
    val myVillages by osmViewModel.myVillages.collectAsStateWithLifecycle()
    val selectedVillage by osmViewModel.selectedVillage.collectAsStateWithLifecycle()
    val households by osmViewModel.householdsInSelectedVillage.collectAsStateWithLifecycle()
    val selectedHousehold by osmViewModel.selectedHousehold.collectAsStateWithLifecycle()
    val persons by osmViewModel.personsInSelectedHousehold.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeUsbNotification by remember { mutableStateOf<UsbConnectionUiEvent?>(null) }

    LaunchedEffect(Unit) {
        viewModel.usbUiEvents.collect { event ->
            activeUsbNotification = event
            // Auto dismiss toast after 4.5 seconds
            delay(4500)
            if (activeUsbNotification == event) {
                activeUsbNotification = null
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart OSM",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "v2.6.0 Pro (OBD-II Engine)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { currentScreen = Screen.DeveloperStory }) {
                        Icon(Icons.Default.Info, contentDescription = "Developer Story", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                listOf(
                    Screen.Dashboard,
                    Screen.DtcScan,
                    Screen.VillageDashboard,
                    Screen.Predictive,
                    Screen.Profile
                ).forEach { screen ->
                    val isSelected = currentScreen.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.titleTh,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        label = {
                            Text(
                                text = screen.titleTh,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${screen.route}")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    telemetry = telemetry,
                    activeMode = activeMode,
                    currentScenario = currentScenario,
                    diagnosticReport = diagnosticReport,
                    connectedDeviceName = connectedUsbDevice?.deviceName,
                    chipsetType = detectedChipset,
                    tripSummary = tripSummary,
                    tripStatus = tripStatus,
                    onStartTrip = { viewModel.startTrip() },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onStopTrip = { viewModel.stopTrip() },
                    onModeSelected = { viewModel.setMode(it) },
                    onConnectUsb = { viewModel.connectUsbHardware() },
                    onDisconnectUsb = { viewModel.disconnectUsbHardware() },
                    onScenarioSelected = { viewModel.setSimulatorScenario(it) },
                    onNavigateToAiMechanic = { currentScreen = Screen.AiMechanic },
                    maintenanceAlerts = activeMaintenanceAlerts,
                    onNavigateToMaintenance = { currentScreen = Screen.Predictive }
                )
                Screen.DtcScan -> DtcScannerScreen(
                    dtcCodes = dtcCodes,
                    isScanning = isScanning,
                    activeMode = activeMode,
                    scanHistory = scanHistory,
                    onStartScan = { viewModel.scanDtcs() },
                    onClearDtcs = { viewModel.clearDtcs() },
                    onLoadCachedScan = { viewModel.loadCachedScanIntoCurrent(it) },
                    onDeleteCachedScan = { viewModel.deleteScanHistoryItem(it) },
                    onClearAllCachedScans = { viewModel.clearAllScanHistory() },
                    onNavigateToAiMechanic = { currentScreen = Screen.AiMechanic },
                    onExportPdfReport = {
                        viewModel.exportDiagnosticPdfReport { file ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (file != null) "สร้างรายงาน PDF เรียบร้อย: ${file.name}"
                                    else "ไม่สามารถสร้างรายงาน PDF ได้"
                                )
                            }
                        }
                    }
                )
                Screen.AiMechanic -> AiMechanicScreen(
                    aiResult = aiResult,
                    isAiAnalyzing = isAiAnalyzing,
                    telemetry = telemetry,
                    dtcCodes = dtcCodes,
                    onRequestAiAnalysis = { viewModel.requestAiMechanicAnalysis() }
                )
                Screen.Predictive -> PredictiveMaintenanceScreen(
                    items = viewModel.getPredictiveMaintenanceItems(),
                    serviceIntervals = serviceIntervals,
                    dtcClearTracker = dtcClearTracker,
                    onMarkServiceCompleted = { interval, cost, notes ->
                        viewModel.markServiceCompleted(interval, cost, notes)
                        scope.launch {
                            snackbarHostState.showSnackbar("บันทึกการบำรุงรักษา ${interval.titleTh} เรียบร้อยแล้ว")
                        }
                    },
                    onAddCustomInterval = { titleTh, titleEn, cat, km, days ->
                        viewModel.addCustomServiceInterval(titleTh, titleEn, cat, km, days)
                        scope.launch {
                            snackbarHostState.showSnackbar("เพิ่มรอบการบำรุงรักษา $titleTh เรียบร้อยแล้ว")
                        }
                    },
                    onUpdateIntervalTarget = { id, km, days ->
                        viewModel.updateServiceIntervalTarget(id, km, days)
                        scope.launch {
                            snackbarHostState.showSnackbar("อัปเดตรอบกำหนดระยะทางเป็น $km กม. / $days วันแล้ว")
                        }
                    },
                    onDeleteInterval = { id ->
                        viewModel.deleteServiceInterval(id)
                        scope.launch {
                            snackbarHostState.showSnackbar("ลบรายการเช็กระยะเรียบร้อย")
                        }
                    },
                    onSimulateDrivingKm = { km ->
                        viewModel.simulateDrivingKm(km)
                        scope.launch {
                            snackbarHostState.showSnackbar("จำลองการขับขี่ +$km กม. — คำนวณรอบระยะทางใหม่แล้ว")
                        }
                    },
                    onTriggerTestNotification = { interval ->
                        viewModel.triggerTestNotification(interval)
                        scope.launch {
                            snackbarHostState.showSnackbar("ส่งการแจ้งเตือนเตือนเช็กระยะไปยังเครื่องแล้ว")
                        }
                    },
                    onTriggerDtcClearNotification = {
                        viewModel.triggerDtcClearNotification()
                        scope.launch {
                            snackbarHostState.showSnackbar("ส่งการแจ้งเตือนสรุปผลการขับขี่หลังลบโค้ดแล้ว")
                        }
                    }
                )
                Screen.Profile -> {
                    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
                    VehicleProfileScreen(
                        profiles = profiles,
                        selectedProfile = selectedProfile,
                        appTheme = appTheme,
                        onThemeChanged = { viewModel.setAppTheme(it) },
                        onSelectProfile = { viewModel.selectVehicleProfile(it) },
                        onAddProfile = { name, make, model, year, engine, plate, mileage ->
                            viewModel.addVehicleProfile(name, make, model, year, engine, plate, mileage)
                        },
                        onAddMaintenanceLog = { title, cost, mileage, category ->
                            viewModel.addMaintenanceLog(title, cost, mileage, category)
                        }
                    )
                }
                Screen.VillageDashboard -> VillageDashboardScreen(
                    villages = myVillages,
                    onVillageClick = { 
                        osmViewModel.selectVillage(it)
                        currentScreen = Screen.HouseholdRegistry
                    },
                    onAddVillage = { id, name, desc -> osmViewModel.addVillage(id, name, desc) },
                    onSyncClick = { osmViewModel.triggerSync() },
                    onAiConsultClick = { osmViewModel.requestAiConsult() }
                )
                Screen.HouseholdRegistry -> selectedVillage?.let { village ->
                    HouseholdRegistryScreen(
                        village = village,
                        households = households,
                        onBackClick = { currentScreen = Screen.VillageDashboard },
                        onHouseholdClick = {
                            osmViewModel.selectHousehold(it)
                            currentScreen = Screen.PersonProfile
                        },
                        onAddHousehold = { houseNo -> osmViewModel.addHousehold(houseNo) }
                    )
                }
                Screen.PersonProfile -> selectedHousehold?.let { household ->
                    PersonProfileScreen(
                        household = household,
                        persons = persons,
                        onBackClick = { currentScreen = Screen.HouseholdRegistry },
                        onAddPerson = { title, first, last, birth ->
                            osmViewModel.addPerson(title, first, last, birth)
                        }
                    )
                }
                Screen.DeveloperStory -> DeveloperStoryScreen(
                    onNavigateBack = { currentScreen = Screen.Dashboard }
                )
            }

            // Floating USB OTG connection/disconnection event notification toast
            UsbConnectionNotificationToast(
                event = activeUsbNotification,
                onDismiss = { activeUsbNotification = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }
}
