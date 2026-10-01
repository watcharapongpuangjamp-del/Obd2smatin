package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.MaintenanceLogEntity
import com.example.db.VehicleProfileEntity
import com.example.hardware.SimulatorScenario
import com.example.model.AiAnalysisResult
import com.example.model.AppOperationMode
import com.example.model.CachedDtcScanHistory
import com.example.model.ConnectionState
import com.example.model.DtcClearTrackerSummary
import com.example.model.DtcCode
import com.example.model.LiveSensorData
import com.example.model.PredictiveMaintenanceItem
import com.example.model.ServiceCategory
import com.example.model.ServiceIntervalUiModel
import com.example.model.ServiceUrgencyStatus
import com.example.notification.ServiceNotificationHelper
import com.example.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = VehicleRepository(application)

    val activeMode: StateFlow<AppOperationMode> = repository.activeMode
    val liveTelemetry: StateFlow<LiveSensorData> = repository.liveTelemetry.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveSensorData.disconnected(AppOperationMode.REAL_HARDWARE, "เริ่มต้นระบบ")
    )

    private val _dtcCodes = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcCodes: StateFlow<List<DtcCode>> = _dtcCodes.asStateFlow()

    val diagnosticRuleReport: StateFlow<com.example.rules.RuleEngineReport> = combine(liveTelemetry, dtcCodes) { telemetry, dtcs ->
        repository.diagnosticRuleEngine.evaluate(telemetry, dtcs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.diagnosticRuleEngine.evaluate(
            LiveSensorData.disconnected(AppOperationMode.REAL_HARDWARE, "เริ่มต้นระบบ"),
            emptyList()
        )
    )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _aiResult = MutableStateFlow<AiAnalysisResult?>(null)
    val aiResult: StateFlow<AiAnalysisResult?> = _aiResult.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    val vehicleProfiles: StateFlow<List<VehicleProfileEntity>> = repository.allProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedProfile = MutableStateFlow<VehicleProfileEntity?>(null)
    val selectedProfile: StateFlow<VehicleProfileEntity?> = _selectedProfile.asStateFlow()

    val currentScenario = repository.emulatorService.currentScenario

    val usbSerialMonitor = repository.usbSerialMonitor
    val usbConnectionState = repository.usbSerialMonitor.connectionState
    val connectedUsbDevice = repository.usbSerialMonitor.connectedDevice
    val detectedChipset = repository.usbSerialMonitor.detectedChipset
    val usbUiEvents = repository.usbSerialMonitor.uiEvents

    val activeTripStatus = repository.tripAnalytics.activeTripStatus
    val currentTripSummary = repository.tripAnalytics.currentTripSummary

    val cachedScanHistory: StateFlow<List<CachedDtcScanHistory>> = combine(
        repository.allScanRecords,
        vehicleProfiles
    ) { records, profiles ->
        val profileMap = profiles.associateBy { it.id }
        records.map { entity ->
            val profile = profileMap[entity.vehicleId]
            val vehicleLabel = profile?.let { "${it.make} ${it.model} (${it.licensePlate})" } ?: "รถยนต์ทั่วไป"
            CachedDtcScanHistory(
                id = entity.id,
                vehicleId = entity.vehicleId,
                vehicleName = vehicleLabel,
                timestamp = entity.timestamp,
                totalCodesFound = entity.totalCodesFound,
                codes = repository.parseDtcCodes(entity.codesJson),
                modeProvenance = entity.modeProvenance,
                notes = entity.notes
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Service Interval Tracking Engine
    val serviceIntervals: StateFlow<List<ServiceIntervalUiModel>> = combine(
        repository.allServiceIntervals,
        selectedProfile,
        liveTelemetry
    ) { entities, profile, telemetry ->
        val vehicleId = profile?.id ?: 1L
        val currentOdo = profile?.odometerKm ?: 125400
        val vehicleIntervals = entities.filter { it.vehicleId == vehicleId }
        val now = System.currentTimeMillis()

        vehicleIntervals.map { entity ->
            val isDtcTrack = entity.trackDtcClear
            val distSince = if (isDtcTrack) {
                (currentOdo - entity.lastDtcClearKm).coerceAtLeast(0)
            } else {
                (currentOdo - entity.lastServiceKm).coerceAtLeast(0)
            }

            val lastTime = if (isDtcTrack && entity.lastDtcClearTimestamp > 0) entity.lastDtcClearTimestamp else entity.lastServiceTimestamp
            val daysSince = if (lastTime > 0) {
                TimeUnit.MILLISECONDS.toDays(now - lastTime).toInt().coerceAtLeast(0)
            } else {
                0
            }

            val remKm = entity.targetIntervalKm - distSince
            val remDays = entity.targetIntervalDays - daysSince

            val kmRatio = if (entity.targetIntervalKm > 0) (distSince.toFloat() / entity.targetIntervalKm.toFloat()).coerceIn(0f, 2f) else 0f
            val daysRatio = if (entity.targetIntervalDays > 0) (daysSince.toFloat() / entity.targetIntervalDays.toFloat()).coerceIn(0f, 2f) else 0f
            val maxRatio = maxOf(kmRatio, daysRatio)

            val urgency = when {
                remKm <= 0 || remDays <= 0 -> ServiceUrgencyStatus.OVERDUE
                remKm <= 1000 || remDays <= 14 -> ServiceUrgencyStatus.DUE_SOON
                else -> ServiceUrgencyStatus.NORMAL
            }

            val categoryEnum = try {
                ServiceCategory.valueOf(entity.category)
            } catch (e: Exception) {
                ServiceCategory.GENERAL
            }

            val statusDesc = when (urgency) {
                ServiceUrgencyStatus.OVERDUE -> {
                    if (remKm <= 0 && remDays <= 0) "เกินกำหนด ${-remKm} กม. และ ${-remDays} วัน (ควรเข้ารับบริการทันที)"
                    else if (remKm <= 0) "เกินกำหนดระยะทาง ${-remKm} กม. แล้ว"
                    else "เกินกำหนดเวลา ${-remDays} วันแล้ว"
                }
                ServiceUrgencyStatus.DUE_SOON -> {
                    "ใกล้ถึงกำหนด (เหลืออีก $remKm กม. หรือ $remDays วัน)"
                }
                ServiceUrgencyStatus.NORMAL -> {
                    "ปกติ (เหลืออีก $remKm กม. หรือ $remDays วัน)"
                }
            }

            ServiceIntervalUiModel(
                id = entity.id,
                vehicleId = entity.vehicleId,
                titleTh = entity.titleTh,
                titleEn = entity.titleEn,
                category = categoryEnum,
                targetIntervalKm = entity.targetIntervalKm,
                targetIntervalDays = entity.targetIntervalDays,
                lastServiceKm = entity.lastServiceKm,
                lastServiceTimestamp = entity.lastServiceTimestamp,
                trackDtcClear = entity.trackDtcClear,
                lastDtcClearKm = entity.lastDtcClearKm,
                lastDtcClearTimestamp = entity.lastDtcClearTimestamp,
                currentOdometerKm = currentOdo,
                distanceSinceLastServiceKm = distSince,
                daysSinceLastService = daysSince,
                remainingKm = remKm,
                remainingDays = remDays,
                progressRatio = maxRatio,
                urgencyStatus = urgency,
                statusDescriptionTh = statusDesc,
                isCustom = entity.isCustom
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // DTC Clear Drive Cycle and Time Tracker Summary
    val dtcClearTracker: StateFlow<DtcClearTrackerSummary> = combine(
        repository.latestDtcClearEvent,
        selectedProfile
    ) { event, profile ->
        val currentOdo = profile?.odometerKm ?: 125400
        val now = System.currentTimeMillis()
        if (event == null) {
            DtcClearTrackerSummary(
                lastClearTimestamp = 0L,
                lastClearOdometerKm = currentOdo,
                currentOdometerKm = currentOdo,
                distanceSinceClearKm = 0,
                timeSinceClearDays = 0,
                timeSinceClearHours = 0L,
                clearedCodesCount = 0,
                clearedCodesList = emptyList(),
                ecuReadinessDriveCycleCompleted = true,
                driveCycleTargetKm = 500,
                driveCycleProgressPercent = 100,
                hasHistory = false
            )
        } else {
            val dist = (currentOdo - event.odometerKm).coerceAtLeast(0)
            val timeDiffMs = (now - event.timestamp).coerceAtLeast(0)
            val days = TimeUnit.MILLISECONDS.toDays(timeDiffMs).toInt()
            val hours = TimeUnit.MILLISECONDS.toHours(timeDiffMs) % 24
            val driveCycleTarget = 500
            val progress = ((dist.toFloat() / driveCycleTarget.toFloat()) * 100).toInt().coerceIn(0, 100)
            val isCompleted = dist >= driveCycleTarget || days >= 7

            val codesList = mutableListOf<String>()
            try {
                val array = JSONArray(event.clearedCodesJson)
                for (i in 0 until array.length()) {
                    codesList.add(array.getString(i))
                }
            } catch (e: Exception) {}

            DtcClearTrackerSummary(
                lastClearTimestamp = event.timestamp,
                lastClearOdometerKm = event.odometerKm,
                currentOdometerKm = currentOdo,
                distanceSinceClearKm = dist,
                timeSinceClearDays = days,
                timeSinceClearHours = hours,
                clearedCodesCount = event.clearedCount,
                clearedCodesList = codesList,
                ecuReadinessDriveCycleCompleted = isCompleted,
                driveCycleTargetKm = driveCycleTarget,
                driveCycleProgressPercent = progress,
                hasHistory = true
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DtcClearTrackerSummary(
            lastClearTimestamp = 0L,
            lastClearOdometerKm = 125400,
            currentOdometerKm = 125400,
            distanceSinceClearKm = 0,
            timeSinceClearDays = 0,
            timeSinceClearHours = 0L,
            clearedCodesCount = 0,
            clearedCodesList = emptyList(),
            ecuReadinessDriveCycleCompleted = true,
            driveCycleTargetKm = 500,
            driveCycleProgressPercent = 100,
            hasHistory = false
        )
    )

    val activeMaintenanceAlerts: StateFlow<List<ServiceIntervalUiModel>> = combine(serviceIntervals) { intervalsArray ->
        intervalsArray.first().filter { it.urgencyStatus != ServiceUrgencyStatus.NORMAL }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Collect telemetry for Trip Analytics
        viewModelScope.launch {
            liveTelemetry.collect { sample ->
                repository.tripAnalytics.ingestTelemetrySample(sample)
            }
        }
        // Initialize default vehicle profile if database is empty and seed service intervals
        viewModelScope.launch {
            repository.allProfiles.collect { profiles ->
                if (profiles.isNotEmpty() && _selectedProfile.value == null) {
                    val defaultProfile = profiles.first()
                    _selectedProfile.value = defaultProfile
                }
            }
        }

        // Seed intervals if current profile has no service intervals
        viewModelScope.launch {
            combine(selectedProfile, repository.allServiceIntervals) { profile, allIntervals ->
                Pair(profile, allIntervals)
            }.collect { (profile, allIntervals) ->
                if (profile != null) {
                    val count = allIntervals.count { it.vehicleId == profile.id }
                    if (count == 0) {
                        repository.seedDefaultServiceIntervalsIfEmpty(profile.id, profile.odometerKm)
                    }
                }
            }
        }
    }

    fun setMode(mode: AppOperationMode) {
        repository.setOperationMode(mode)
    }

    fun connectUsbHardware() {
        viewModelScope.launch {
            repository.connectRealHardware()
        }
    }

    fun disconnectUsbHardware() {
        repository.disconnectRealHardware()
    }

    fun setSimulatorScenario(scenario: SimulatorScenario) {
        repository.setSimulatorScenario(scenario)
    }

    fun scanDtcs() {
        viewModelScope.launch {
            _isScanning.value = true
            val codes = repository.scanDtcs()
            _dtcCodes.value = codes
            _isScanning.value = false

            val vehicleId = _selectedProfile.value?.id ?: 1L
            repository.saveScanRecord(vehicleId, codes)
        }
    }

    fun loadCachedScanIntoCurrent(scan: CachedDtcScanHistory) {
        _dtcCodes.value = scan.codes
        _aiResult.value = null
    }

    fun deleteScanHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteScanRecord(id)
        }
    }

    fun clearAllScanHistory() {
        viewModelScope.launch {
            repository.clearAllScanRecords()
        }
    }

    fun clearDtcs() {
        viewModelScope.launch {
            _isScanning.value = true
            val previousCodes = _dtcCodes.value
            repository.clearDtcs()
            val vehicle = _selectedProfile.value
            val vehicleId = vehicle?.id ?: 1L
            val currentOdo = vehicle?.odometerKm ?: 125400
            repository.recordDtcClearEvent(
                vehicleId = vehicleId,
                odometerKm = currentOdo,
                clearedCodes = previousCodes,
                notes = "ลบรหัสความผิดปกติผ่าน OBD-II Diagnostic Scanner"
            )
            _dtcCodes.value = emptyList()
            _aiResult.value = null
            _isScanning.value = false

            // Send notification for DTC Clear and Drive Cycle monitoring
            ServiceNotificationHelper.sendDtcClearVerificationAlert(
                context = getApplication(),
                vehicleName = vehicle?.let { "${it.make} ${it.model}" } ?: "รถยนต์",
                distanceKm = 0,
                clearedCount = previousCodes.size
            )
        }
    }

    fun markServiceCompleted(interval: ServiceIntervalUiModel, costBaht: Double = 0.0, notes: String = "") {
        viewModelScope.launch {
            val vehicleId = _selectedProfile.value?.id ?: 1L
            val currentOdo = _selectedProfile.value?.odometerKm ?: 125400
            repository.recordServiceCompleted(
                intervalId = interval.id,
                vehicleId = vehicleId,
                titleTh = interval.titleTh,
                costBaht = costBaht,
                currentOdoKm = currentOdo,
                category = interval.category.name,
                notes = notes.ifBlank { "บันทึกเปลี่ยนอะไหล่/เช็กระยะเรียบร้อยแล้ว" }
            )
        }
    }

    fun addCustomServiceInterval(
        titleTh: String,
        titleEn: String,
        category: ServiceCategory,
        targetKm: Int,
        targetDays: Int
    ) {
        viewModelScope.launch {
            val vehicleId = _selectedProfile.value?.id ?: 1L
            val currentOdo = _selectedProfile.value?.odometerKm ?: 125400
            repository.addCustomServiceInterval(
                vehicleId = vehicleId,
                titleTh = titleTh,
                titleEn = titleEn,
                category = category.name,
                targetKm = targetKm,
                targetDays = targetDays,
                currentOdoKm = currentOdo
            )
        }
    }

    fun updateServiceIntervalTarget(intervalId: Long, targetKm: Int, targetDays: Int) {
        viewModelScope.launch {
            repository.updateServiceIntervalTarget(intervalId, targetKm, targetDays)
        }
    }

    fun deleteServiceInterval(intervalId: Long) {
        viewModelScope.launch {
            repository.deleteServiceInterval(intervalId)
        }
    }

    fun simulateDrivingKm(additionalKm: Int) {
        val current = _selectedProfile.value ?: return
        val newOdo = current.odometerKm + additionalKm
        val updated = current.copy(odometerKm = newOdo)
        _selectedProfile.value = updated
        viewModelScope.launch {
            repository.updateVehicleOdometer(current.id, newOdo)
        }
    }

    fun triggerTestNotification(interval: ServiceIntervalUiModel) {
        val vehicleName = _selectedProfile.value?.let { "${it.make} ${it.model} (${it.licensePlate})" } ?: "รถของคุณ"
        ServiceNotificationHelper.sendServiceAlertNotification(
            context = getApplication(),
            interval = interval,
            vehicleName = vehicleName
        )
    }

    fun triggerDtcClearNotification() {
        val vehicleName = _selectedProfile.value?.let { "${it.make} ${it.model}" } ?: "รถของคุณ"
        val tracker = dtcClearTracker.value
        ServiceNotificationHelper.sendDtcClearVerificationAlert(
            context = getApplication(),
            vehicleName = vehicleName,
            distanceKm = tracker.distanceSinceClearKm,
            clearedCount = tracker.clearedCodesCount
        )
    }

    fun requestAiMechanicAnalysis() {
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            val profileInfo = _selectedProfile.value?.let {
                "${it.make} ${it.model} ปี ${it.year} เครื่องยนต์ ${it.engineType} เลขกิโลเมตร ${it.odometerKm} กม."
            } ?: "รถยนต์ทั่วไป"

            val result = repository.analyzeWithAiMechanic(
                vehicleInfo = profileInfo,
                dtcCodes = _dtcCodes.value,
                telemetry = liveTelemetry.value
            )
            _aiResult.value = result
            _isAiAnalyzing.value = false
        }
    }

    fun getPredictiveMaintenanceItems(): List<PredictiveMaintenanceItem> {
        return repository.getPredictiveMaintenanceList(liveTelemetry.value)
    }

    fun selectVehicleProfile(profile: VehicleProfileEntity) {
        _selectedProfile.value = profile
    }

    fun addVehicleProfile(name: String, make: String, model: String, year: Int, engine: String, plate: String, mileage: Int) {
        viewModelScope.launch {
            val newProfile = VehicleProfileEntity(
                name = name,
                make = make,
                model = model,
                year = year,
                engineType = engine,
                licensePlate = plate,
                odometerKm = mileage
            )
            val id = repository.saveProfile(newProfile)
            _selectedProfile.value = newProfile.copy(id = id)
        }
    }

    fun addMaintenanceLog(title: String, cost: Double, mileage: Int, category: String) {
        val currentVehicleId = _selectedProfile.value?.id ?: 1L
        viewModelScope.launch {
            val log = MaintenanceLogEntity(
                vehicleId = currentVehicleId,
                titleTh = title,
                dateTimestamp = System.currentTimeMillis(),
                costBaht = cost,
                mileageKm = mileage,
                category = category
            )
            repository.saveMaintenanceLog(log)
        }
    }

    fun startTrip() {
        repository.tripAnalytics.startOrResumeTrip(activeMode.value)
    }

    fun pauseTrip() {
        repository.tripAnalytics.pauseTrip()
    }

    fun stopTrip(): com.example.model.TripSummary? {
        return repository.tripAnalytics.resetAndEndTrip()
    }

    private val _lastExportedPdfPath = MutableStateFlow<String?>(null)
    val lastExportedPdfPath: StateFlow<String?> = _lastExportedPdfPath.asStateFlow()

    fun exportDiagnosticPdfReport(onComplete: ((java.io.File?) -> Unit)? = null) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val profile = _selectedProfile.value
            val currentTelemetry = liveTelemetry.value
            val currentDtcs = _dtcCodes.value
            val currentRuleReport = diagnosticRuleReport.value
            val currentAi = _aiResult.value

            val session = com.example.model.DiagnosticSession(
                sessionId = "DS-${System.currentTimeMillis() % 1000000}",
                timestamp = System.currentTimeMillis(),
                vehicleName = profile?.name ?: "รถยนต์ทดสอบ",
                vehicleMake = profile?.make ?: "Toyota",
                vehicleModel = profile?.model ?: "Hilux Revo",
                vehicleYear = profile?.year ?: 2022,
                vehicleVin = "MHFAB22G0K${100000 + (System.currentTimeMillis() % 900000)}",
                licensePlate = profile?.licensePlate ?: "1กข-9999 กทม.",
                odometerKm = profile?.odometerKm ?: 125400,
                dtcCodes = currentDtcs,
                telemetrySnapshot = currentTelemetry,
                ruleReport = currentRuleReport,
                aiAnalysis = currentAi,
                technicianName = "ช่างผู้ตรวจสอบระบบ Thai OBD-II Pro",
                mode = activeMode.value
            )

            val exportDir = java.io.File(context.cacheDir, "reports")
            exportDir.mkdirs()
            val targetFile = java.io.File(exportDir, "Diagnostic_Report_${session.sessionId}.pdf")

            val result = repository.pdfExporter.exportToFile(session, targetFile)
            if (result.isSuccess) {
                _lastExportedPdfPath.value = targetFile.absolutePath
                onComplete?.invoke(targetFile)
            } else {
                onComplete?.invoke(null)
            }
        }
    }

    override fun onCleared() {

        super.onCleared()
        repository.release()
    }
}
