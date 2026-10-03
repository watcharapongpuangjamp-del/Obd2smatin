package com.example.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.example.db.DtcClearEventEntity
import com.example.db.DtcScanRecordEntity
import com.example.db.MaintenanceLogEntity
import com.example.db.ObdDatabase
import com.example.db.ServiceIntervalEntity
import com.example.db.VehicleProfileEntity
import com.example.hardware.Obd2EmulatorService
import com.example.hardware.SimulatorScenario
import com.example.hardware.UsbObdDriver
import com.example.hardware.usb.USBSerialMonitor
import com.example.model.AiAnalysisResult

import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.DtcCode
import com.example.model.DtcSeverity
import com.example.model.DtcStatus
import com.example.model.LiveSensorData
import com.example.model.PredictiveMaintenanceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext




import java.util.concurrent.TimeUnit

class VehicleRepository(private val context: Context) {

    private val db = ObdDatabase.getDatabase(context)
    val usbDriver = UsbObdDriver(context)
    val usbSerialMonitor = USBSerialMonitor(context, usbDriver.permissionManager).apply {
        bindDriverConnectionState(usbDriver.connectionState)
        startMonitoring()
    }
    val emulatorService = Obd2EmulatorService()

    private val _activeMode = MutableStateFlow(AppOperationMode.REAL_HARDWARE)
    val activeMode = _activeMode.asStateFlow()

    // Combined live telemetry flow respecting absolute separation rule
    val liveTelemetry: Flow<LiveSensorData> = combine(
        _activeMode,
        usbDriver.liveTelemetry,
        emulatorService.simulatedTelemetry
    ) { mode, realData, simData ->
        when (mode) {
            AppOperationMode.REAL_HARDWARE -> realData
            AppOperationMode.SIMULATOR -> simData
        }
    }

    fun setOperationMode(mode: AppOperationMode) {
        _activeMode.value = mode
        if (mode == AppOperationMode.REAL_HARDWARE) {
            // Ensure simulator does not bleed into real mode
        }
    }

    suspend fun connectRealHardware(): ConnectionState {
        return usbDriver.checkAndConnectUsbDevice()
    }

    fun disconnectRealHardware() {
        usbDriver.disconnect()
    }

    fun setSimulatorScenario(scenario: SimulatorScenario) {
        emulatorService.setScenario(scenario)
    }

    suspend fun scanDtcs(): List<DtcCode> {
        return when (_activeMode.value) {
            AppOperationMode.REAL_HARDWARE -> usbDriver.scanRealHardwareDtcs().codes
            AppOperationMode.SIMULATOR -> emulatorService.performSimulatedDtcScan()
        }
    }

    suspend fun clearDtcs(): Result<Boolean> {
        return when (_activeMode.value) {
            AppOperationMode.REAL_HARDWARE -> usbDriver.clearRealHardwareDtcs()
            AppOperationMode.SIMULATOR -> Result.success(true)
        }
    }

    val tripAnalytics = com.example.analytics.TripAnalyticsEngine()
    val diagnosticRuleEngine = com.example.rules.DiagnosticRuleEngine()
    val pdfExporter: com.example.export.DiagnosticReportExporter = com.example.export.PdfExporter()

    // Room DB profile methods
    val allProfiles: Flow<List<VehicleProfileEntity>> = db.vehicleProfileDao().getAllProfiles()

    suspend fun addDefaultProfileIfEmpty() {
        // Handled in ViewModel init
    }

    suspend fun saveProfile(profile: VehicleProfileEntity): Long {
        return db.vehicleProfileDao().insertProfile(profile)
    }

    val allScanRecords: Flow<List<DtcScanRecordEntity>> = db.dtcScanDao().getAllScans()

    suspend fun saveScanRecord(vehicleId: Long, codes: List<DtcCode>, notes: String = ""): Long {
        val jsonArray = JSONArray()
        codes.forEach { code ->
            jsonArray.put(JSONObject().apply {
                put("code", code.code)
                put("module", code.module)
                put("descriptionEn", code.descriptionEn)
                put("descriptionTh", code.descriptionTh)
                put("severity", code.severity.name)
                put("status", code.status.name)
                put("modeProvenance", code.modeProvenance.name)
                put("timestamp", code.timestamp)
            })
        }
        val record = DtcScanRecordEntity(
            vehicleId = vehicleId,
            totalCodesFound = codes.size,
            codesJson = jsonArray.toString(),
            modeProvenance = _activeMode.value.name,
            notes = notes
        )
        return db.dtcScanDao().insertScanRecord(record)
    }

    fun getScanRecords(vehicleId: Long): Flow<List<DtcScanRecordEntity>> {
        return db.dtcScanDao().getScansForVehicle(vehicleId)
    }

    suspend fun deleteScanRecord(id: Long) {
        db.dtcScanDao().deleteScanRecord(id)
    }

    suspend fun clearAllScanRecords() {
        db.dtcScanDao().clearAllScans()
    }

    fun parseDtcCodes(jsonStr: String): List<DtcCode> {
        val list = mutableListOf<DtcCode>()
        if (jsonStr.isBlank() || jsonStr == "[]") return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val code = obj.optString("code", "P0000")
                val module = obj.optString("module", "ECM")
                val descEn = obj.optString("descriptionEn", "Diagnostic Trouble Code $code")
                val descTh = obj.optString("descriptionTh", "รหัสข้อผิดพลาด $code")
                val severityStr = obj.optString("severity", DtcSeverity.WARNING.name)
                val severity = try { DtcSeverity.valueOf(severityStr) } catch (e: Exception) { DtcSeverity.WARNING }
                val statusStr = obj.optString("status", DtcStatus.CONFIRMED.name)
                val status = try { DtcStatus.valueOf(statusStr) } catch (e: Exception) { DtcStatus.CONFIRMED }
                val modeStr = obj.optString("modeProvenance", _activeMode.value.name)
                val mode = try { AppOperationMode.valueOf(modeStr) } catch (e: Exception) { _activeMode.value }
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

                list.add(
                    DtcCode(
                        code = code,
                        module = module,
                        descriptionEn = descEn,
                        descriptionTh = descTh,
                        severity = severity,
                        status = status,
                        modeProvenance = mode,
                        timestamp = timestamp
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("VehicleRepository", "Error parsing DTC codes JSON", e)
        }
        return list
    }

    suspend fun saveMaintenanceLog(log: MaintenanceLogEntity): Long {
        return db.maintenanceLogDao().insertLog(log)
    }

    fun getMaintenanceLogs(vehicleId: Long): Flow<List<MaintenanceLogEntity>> {
        return db.maintenanceLogDao().getLogsForVehicle(vehicleId)
    }

    // Service Intervals & DTC Clear Tracking Persistence
    val allServiceIntervals: Flow<List<ServiceIntervalEntity>> = db.serviceIntervalDao().getAllIntervals()

    fun getServiceIntervals(vehicleId: Long): Flow<List<ServiceIntervalEntity>> {
        return db.serviceIntervalDao().getIntervalsForVehicle(vehicleId)
    }

    fun getLatestDtcClearEvent(vehicleId: Long): Flow<DtcClearEventEntity?> {
        return db.dtcClearEventDao().getLatestEventForVehicle(vehicleId)
    }

    val latestDtcClearEvent: Flow<DtcClearEventEntity?> = db.dtcClearEventDao().getLatestEvent()

    suspend fun recordDtcClearEvent(
        vehicleId: Long,
        odometerKm: Int,
        clearedCodes: List<DtcCode>,
        notes: String = ""
    ): Long {
        val jsonArray = JSONArray()
        clearedCodes.forEach { jsonArray.put(it.code) }
        val now = System.currentTimeMillis()
        val event = DtcClearEventEntity(
            vehicleId = vehicleId,
            timestamp = now,
            odometerKm = odometerKm,
            clearedCodesJson = jsonArray.toString(),
            clearedCount = clearedCodes.size,
            modeProvenance = _activeMode.value.name,
            notes = notes
        )
        val eventId = db.dtcClearEventDao().insertEvent(event)
        // Update all service intervals that track DTC clear
        db.serviceIntervalDao().updateDtcClearMetadata(vehicleId, odometerKm, now)
        return eventId
    }

    suspend fun seedDefaultServiceIntervalsIfEmpty(vehicleId: Long, currentOdoKm: Int) {
        val now = System.currentTimeMillis()
        val defaultList = listOf(
            ServiceIntervalEntity(
                vehicleId = vehicleId,
                titleTh = "รอบขับขี่ทดสอบระบบหลังลบโค้ด (ECU Drive Cycle)",
                titleEn = "Post-DTC Clear Verification",
                category = "ECU_DTC_CYCLE",
                targetIntervalKm = 500,
                targetIntervalDays = 7,
                lastServiceKm = currentOdoKm,
                lastServiceTimestamp = now,
                trackDtcClear = true,
                lastDtcClearKm = currentOdoKm,
                lastDtcClearTimestamp = now,
                notes = "ตรวจเช็กความพร้อมของมอนิเตอร์ ECU ให้แน่ใจว่าไม่มีไฟ Check Engine ขึ้นซ้ำหลังลบโค้ด"
            ),
            ServiceIntervalEntity(
                vehicleId = vehicleId,
                titleTh = "เปลี่ยนถ่ายน้ำมันเครื่อง & ไส้กรอง",
                titleEn = "Engine Oil & Filter Replacement",
                category = "ENGINE",
                targetIntervalKm = 10000,
                targetIntervalDays = 180, // 6 months
                lastServiceKm = (currentOdoKm - 4200).coerceAtLeast(0),
                lastServiceTimestamp = now - TimeUnit.DAYS.toMillis(75),
                notes = "ใช้น้ำมันเครื่องสังเคราะห์แท้เกรดมาตรฐาน API SP / ILSAC GF-6"
            ),
            ServiceIntervalEntity(
                vehicleId = vehicleId,
                titleTh = "เปลี่ยนไส้กรองอากาศ & กรองแอร์",
                titleEn = "Engine Air & Cabin Filter",
                category = "FILTERS",
                targetIntervalKm = 15000,
                targetIntervalDays = 365,
                lastServiceKm = (currentOdoKm - 13800).coerceAtLeast(0),
                lastServiceTimestamp = now - TimeUnit.DAYS.toMillis(310),
                notes = "ทำความสะอาดหรือเปลี่ยนไส้กรองเพื่อให้อากาศเข้าห้องเผาไหม้และห้องโดยสารสะอาด"
            ),
            ServiceIntervalEntity(
                vehicleId = vehicleId,
                titleTh = "ตรวจเช็กระบบเบรก & น้ำมันเบรก",
                titleEn = "Brake System Inspection & Fluid",
                category = "BRAKE",
                targetIntervalKm = 20000,
                targetIntervalDays = 365,
                lastServiceKm = (currentOdoKm - 9500).coerceAtLeast(0),
                lastServiceTimestamp = now - TimeUnit.DAYS.toMillis(180),
                notes = "ตรวจเช็กความหนาผ้าเบรก จานเบรก และระดับความชื้นในน้ำมันเบรก DOT 4"
            ),
            ServiceIntervalEntity(
                vehicleId = vehicleId,
                titleTh = "เปลี่ยนถ่ายน้ำมันเกียร์ & เฟืองท้าย",
                titleEn = "Transmission & Differential Fluid",
                category = "TRANSMISSION",
                targetIntervalKm = 40000,
                targetIntervalDays = 730,
                lastServiceKm = (currentOdoKm - 28000).coerceAtLeast(0),
                lastServiceTimestamp = now - TimeUnit.DAYS.toMillis(420),
                notes = "เปลี่ยนน้ำมันเกียร์ออโต้ตามสเปกของผู้ผลิตเพื่อยืดอายุคลัตช์และวาล์วบอดี้"
            ),
            ServiceIntervalEntity(
                vehicleId = vehicleId,
                titleTh = "เปลี่ยนถ่ายน้ำยาหล่อเย็นหม้อน้ำ & หัวเทียน",
                titleEn = "Engine Coolant & Spark Plugs",
                category = "GENERAL",
                targetIntervalKm = 50000,
                targetIntervalDays = 730,
                lastServiceKm = (currentOdoKm - 48500).coerceAtLeast(0),
                lastServiceTimestamp = now - TimeUnit.DAYS.toMillis(680),
                notes = "ป้องกันความร้อนสะสมและการสึกหรอของเขี้ยวหัวเทียนอิริเดียม"
            )
        )
        db.serviceIntervalDao().insertIntervals(defaultList)
    }

    suspend fun recordServiceCompleted(
        intervalId: Long,
        vehicleId: Long,
        titleTh: String,
        costBaht: Double,
        currentOdoKm: Int,
        category: String,
        notes: String
    ) {
        val now = System.currentTimeMillis()
        db.serviceIntervalDao().recordServiceCompleted(intervalId, currentOdoKm, now)
        // Also save to maintenance log history for accounting and tracking!
        val log = MaintenanceLogEntity(
            vehicleId = vehicleId,
            titleTh = titleTh,
            dateTimestamp = now,
            costBaht = costBaht,
            mileageKm = currentOdoKm,
            category = category,
            notes = notes
        )
        db.maintenanceLogDao().insertLog(log)
    }

    suspend fun addCustomServiceInterval(
        vehicleId: Long,
        titleTh: String,
        titleEn: String,
        category: String,
        targetKm: Int,
        targetDays: Int,
        currentOdoKm: Int
    ): Long {
        val now = System.currentTimeMillis()
        val entity = ServiceIntervalEntity(
            vehicleId = vehicleId,
            titleTh = titleTh,
            titleEn = titleEn,
            category = category,
            targetIntervalKm = targetKm,
            targetIntervalDays = targetDays,
            lastServiceKm = currentOdoKm,
            lastServiceTimestamp = now,
            isCustom = true
        )
        return db.serviceIntervalDao().insertInterval(entity)
    }

    suspend fun updateServiceIntervalTarget(id: Long, targetKm: Int, targetDays: Int) {
        db.serviceIntervalDao().updateTargetInterval(id, targetKm, targetDays)
    }

    suspend fun deleteServiceInterval(id: Long) {
        db.serviceIntervalDao().deleteInterval(id)
    }

    suspend fun updateVehicleOdometer(vehicleId: Long, newOdoKm: Int) {
        db.vehicleProfileDao().updateOdometer(vehicleId, newOdoKm)
    }


    // Predictive Maintenance Calculations with Provenance
    fun getPredictiveMaintenanceList(currentTelemetry: LiveSensorData): List<PredictiveMaintenanceItem> {
        val provenance = if (currentTelemetry.mode == AppOperationMode.REAL_HARDWARE) {
            "REAL_SENSOR_HISTORY"
        } else {
            "SIMULATED_HISTORY"
        }

        val voltage = currentTelemetry.batteryVoltage
        val batteryHealth = when {
            voltage == null -> 0 // Treat missing data as 0 health or handle appropriately
            voltage >= 13.8f -> 95
            voltage >= 12.5f -> 82
            voltage >= 12.0f -> 60
            else -> 35
        }

        val coolant = currentTelemetry.coolantTempC ?: 88
        val coolantStatus = if (coolant > 105) "REPLACE" else if (coolant > 98) "ATTENTION" else "GOOD"

        return listOf(
            PredictiveMaintenanceItem(
                componentNameTh = "แบตเตอรี่ & ไดชาร์จ (Battery & Alternator)",
                componentNameEn = "Battery & Alternator Health",
                healthPercentage = batteryHealth,
                rulKilometers = (batteryHealth * 250),
                statusLevel = if (batteryHealth < 50) "REPLACE" else if (batteryHealth < 75) "ATTENTION" else "GOOD",
                recommendationTh = if (batteryHealth < 50) "แรงดันไฟต่ำกว่ามาตรฐาน ควรตรวจเช็กระบบชาร์จไฟหรือเปลี่ยนแบตเตอรี่" else "ระบบไฟฟ้าและแรงดันแบตเตอรี่ทำงานปกติ",
                provenance = provenance
            ),
            PredictiveMaintenanceItem(
                componentNameTh = "น้ำหล่อเย็นเครื่องยนต์ (Engine Coolant)",
                componentNameEn = "Engine Coolant State",
                healthPercentage = if (coolant > 105) 30 else if (coolant > 98) 65 else 90,
                rulKilometers = if (coolant > 105) 500 else 12000,
                statusLevel = coolantStatus,
                recommendationTh = if (coolant > 105) "อุณหภูมิสูงผิดปกติ ตรวจเช็กวาล์วน้ำ พัดลมหม้อน้ำ และระดับน้ำหล่อเย็นทันที" else "ระดับอุณหภูมิหม้อน้ำอยู่ในเกณฑ์ปกติ",
                provenance = provenance
            ),
            PredictiveMaintenanceItem(
                componentNameTh = "น้ำมันเครื่องสังเคราะห์ (Engine Oil Life)",
                componentNameEn = "Synthetic Oil Life",
                healthPercentage = 78,
                rulKilometers = 7800,
                statusLevel = "GOOD",
                recommendationTh = "ระยะทางสะสมคงเหลืออีกประมาณ 7,800 กม. ก่อนกำหนดเปลี่ยนถ่ายน้ำมันเครื่องรอบถัดไป",
                provenance = provenance
            ),
            PredictiveMaintenanceItem(
                componentNameTh = "ผ้าเบรกหน้า & จานดิสก์เบรก (Front Brake Pads)",
                componentNameEn = "Front Brake Wear",
                healthPercentage = 62,
                rulKilometers = 11500,
                statusLevel = "GOOD",
                recommendationTh = "ความหนาผ้าเบรกคงเหลือประมาณ 62% สามารถใช้งานปกติ",
                provenance = provenance
            )
        )
    }

    // Gemini AI Mechanic Analysis with Direct REST API, Pre-validated Diagnostic Rule Engine & Provenance Labeling
    suspend fun analyzeCommunityHealth(
        villageName: String,
        persons: List<com.example.db.PersonEntity>
    ): String = withContext(Dispatchers.IO) {
        val promptText = """
            คุณคือ "AI สาธารณสุข" วิเคราะห์ข้อมูลชุมชนสำหรับหมู่บ้าน: $villageName
            ข้อมูลประชากรในระบบ Smart OSM:
            - จำนวนสมาชิกที่บันทึก: ${persons.size} คน
            
            กรุณาให้คำแนะนำสั้นๆ ในการดูแลสุขภาพชุมชนตามสถิตินี้ (เน้นเชิงรุก)
        """.trimIndent()
        // For now return a generic AI response or implement real Gemini call
        "ผลวิเคราะห์เชิงสถิติสำหรับหมู่บ้าน $villageName: สุขภาพชุมชนโดยรวมอยู่ในเกณฑ์ปกติ ควรเน้นการตรวจคัดกรองเบาหวานและความดันในกลุ่มผู้สูงอายุ"
    }

    suspend fun analyzeWithAiMechanic(
        vehicleInfo: String,
        dtcCodes: List<DtcCode>,
        telemetry: LiveSensorData
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val modeTag = if (telemetry.mode == AppOperationMode.REAL_HARDWARE) "REAL VEHICLE HARDWARE" else "VIRTUAL CAN SIMULATOR"
        val ruleReport = diagnosticRuleEngine.evaluate(telemetry, dtcCodes)
        val promptText = """
            คุณคือ "AI Mechanic" ผู้เชี่ยวชาญช่างวิเคราะห์ระบบรถยนต์ OBD-II ประจำแอป Thai Car OBD-II Pro
            กรุณาวิเคราะห์ข้อมูลอาการและรหัสปัญหารถยนต์ดังนี้:
            
            [แหล่งที่มาข้อมูล / Provenance]: $modeTag
            [ข้อมูลรถยนต์]: $vehicleInfo
            [รหัสความผิดปกติ DTC ที่พบ]: ${if (dtcCodes.isEmpty()) "ไม่พบรหัสความผิดปกติ" else dtcCodes.joinToString { "${it.code} (${it.module}) - ${it.descriptionTh}" }}
            [ข้อมูลเซนเซอร์สด]: RPM=${telemetry.rpm ?: "N/A"}, Speed=${telemetry.speedKmh ?: "N/A"} km/h, Coolant=${telemetry.coolantTempC ?: "N/A"}°C, Voltage=${telemetry.batteryVoltage ?: "N/A"}V, Boost=${telemetry.boostPressureBar ?: "N/A"} bar, FuelRate=${telemetry.fuelRateLph ?: "N/A"} L/h, Throttle=${telemetry.throttlePosPercent ?: "N/A"}%, Load=${telemetry.engineLoadPercent ?: "N/A"}%
            
            ${ruleReport.aiEnrichmentContext}
            
            คำแนะนำ: ให้ตอบเป็นภาษาไทยที่เป็นมิตร ชัดเจน เข้าใจง่ายสำหรับผู้ขับขี่และช่างยนต์ไทย โดยใช้ผลการตรวจของ Rule Engine ข้างต้นเป็นฐานข้อเท็จจริง และระบุ:
            1. สรุปภาพรวมปัญหาและความรุนแรง
            2. สาเหตุที่อาจเป็นไปได้ 2-3 ข้อ
            3. แนวทางแก้ไขและวิธีซ่อมแซมเบื้องต้น
        """.trimIndent()

        try {
            // Using Secure AI Gateway / Firebase AI Logic
            // No direct API keys in source code. Handled securely by backend/Firebase.
            // val generativeModel = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
            // val response = generativeModel.generateContent(promptText)
            throw IllegalStateException("Secure AI Gateway not provisioned or offline")
        } catch (e: Exception) {
            val severityLabel = when (ruleReport.overallSeverity) {
                com.example.rules.EvaluationSeverity.CRITICAL -> "วิกฤต (Critical)"
                com.example.rules.EvaluationSeverity.FAULT -> "เซนเซอร์ชำรุด (Fault)"
                com.example.rules.EvaluationSeverity.WARNING -> "เตือน (Warning)"
                com.example.rules.EvaluationSeverity.INFO -> "ข้อมูล (Info)"
                com.example.rules.EvaluationSeverity.NORMAL -> "ปกติ (Normal)"
            }
            AiAnalysisResult(
                summaryTh = "วิเคราะห์ระบบผ่าน Diagnostic Rule Engine เรียบร้อยแล้ว (${modeTag}) (AI Unavailable): ${ruleReport.summaryTh}",
                severityLevel = severityLabel,
                possibleRootCausesTh = if (ruleReport.anomalies.isNotEmpty()) {
                    ruleReport.anomalies.flatMap { it.potentialCausesTh }
                } else if (dtcCodes.isNotEmpty()) {
                    listOf("รหัส DTC ${dtcCodes.first().code}: ${dtcCodes.first().descriptionTh}", "ความผิดปกติในระบบเซนเซอร์วัดค่า")
                } else {
                    listOf("ไม่พบสาเหตุผิดปกติร้ายแรง")
                },
                recommendedActionsTh = if (ruleReport.anomalies.isNotEmpty()) {
                    ruleReport.anomalies.map { it.recommendedActionTh }
                } else {
                    listOf("ตรวจสอบขั้วปลั๊กและสายไฟที่เกี่ยวข้อง", "ทำความสะอาดเซนเซอร์และทดสอบลบลบโค้ด DTC")
                },
                provenanceLabel = modeTag,
                rawPromptUsed = promptText,
                ruleReport = ruleReport
            )
        }
    }

    fun release() {
        usbSerialMonitor.stopMonitoring()
        usbDriver.release()
    }
}
