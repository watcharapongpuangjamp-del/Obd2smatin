package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.model.DtcCode
import com.example.model.LiveSensorData
import com.example.model.VehicleInfo

object GeminiShareUtil {

    /**
     * สร้าง Prompt ระดับพรีเมียมสำหรับ Gemini Pro / Advanced
     */
    fun formatDiagnosticPrompt(
        dtcCodes: List<DtcCode>,
        vehicle: VehicleInfo,
        telemetry: LiveSensorData? = null
    ): String {
        val dtcListStr = if (dtcCodes.isNotEmpty()) {
            dtcCodes.joinToString(separator = "\n") { "- ${it.code} (${it.module}): ${it.descriptionTh} [${it.descriptionEn}]" }
        } else {
            "- ไม่พบรหัสข้อผิดพลาด (Normal Operation)"
        }

        val telemetryStr = if (telemetry != null && telemetry.isConnected) {
            """
            
            ข้อมูลเซนเซอร์สด (Live Telemetry):
            - ความเร็วรอบเครื่องยนต์ (RPM): ${telemetry.rpm ?: "N/A"} RPM
            - อุณหภูมิน้ำหล่อเย็น (Coolant Temp): ${telemetry.coolantTempC ?: "N/A"} °C
            - แรงดันแบตเตอรี่ (Battery Voltage): ${telemetry.batteryVoltage?.let { "%.2f".format(it) } ?: "N/A"} V
            - ความเร็วรถยนต์ (Speed): ${telemetry.speedKmh ?: "N/A"} km/h
            - แรงดันบูสต์ / MAP: ${telemetry.boostPressureBar?.let { "%.2f".format(it) } ?: "N/A"} Bar (${telemetry.mapPressureKpa ?: "N/A"} kPa)
            - ตำแหน่งลิ้นปีกผีเสื้อ (Throttle): ${telemetry.throttlePosPercent ?: "N/A"} %
            - อุณหภูมิไอดี (Intake Air Temp): ${telemetry.intakeTempC ?: "N/A"} °C
            - โหลดเครื่องยนต์ (Engine Load): ${telemetry.engineLoadPercent ?: "N/A"} %
            """.trimIndent()
        } else ""

        return """
            [Thai Car OBD-II Pro: Advanced AI Mechanic Diagnostic Request]
            ข้อมูลรถยนต์:
            - ยี่ห้อ/รุ่น: ${vehicle.brand} ${vehicle.model}
            - ปีผลิต: ${vehicle.year}

            รหัสข้อผิดพลาด (DTC) ที่สแกนพบจากกล่อง ECU:
            $dtcListStr$telemetryStr

            ขอความอนุเคราะห์จากผู้เชี่ยวชาญ AI ระดับพรีเมียม (Gemini Pro/Advanced):
            1. วิเคราะห์เจาะลึกถึงสาเหตุรากเหง้า (Root Cause) ของรหัสปัญหาดังกล่าวในรถยนต์รุ่นนี้
            2. ขั้นตอนการตรวจสอบและวัดค่าด้วยเครื่องมือช่าง (Troubleshooting & Testing Steps)
            3. แนวทางการซ่อมแซม วิธีแก้ปัญหา และชิ้นส่วนอะไหล่ที่ต้องเปลี่ยน
            4. ประมาณการค่าใช้จ่าย ค่าแรง และค่าอะไหล่ในประเทศไทย (ระบุเป็นบาทไทย)
            5. ระดับความเร่งด่วนและความปลอดภัยในการขับขี่ต่อ (Severity & Safety Warning)

            โปรดตอบกลับเป็นภาษาไทยที่กระชับ เป็นมืออาชีพ และเข้าใจง่ายสำหรับช่างยนต์
        """.trimIndent()
    }

    /**
     * คัดลอก Prompt ลงคลิปบอร์ดและเปิดแอปพลิเคชันหรือเว็บ Gemini
     */
    fun copyPromptAndOpenGemini(
        context: Context,
        dtcCodes: List<DtcCode>,
        vehicle: VehicleInfo,
        telemetry: LiveSensorData? = null
    ) {
        val prompt = formatDiagnosticPrompt(dtcCodes, vehicle, telemetry)

        // 1. Copy to Clipboard
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("Thai Car OBD-II Gemini Prompt", prompt)
        clipboard?.setPrimaryClip(clip)

        // 2. Open Gemini App or Web
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/"))

        try {
            context.startActivity(intent)
            Toast.makeText(context, "คัดลอก Prompt วินิจฉัยลงคลิปบอร์ด และเปิด Gemini แล้ว", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "คัดลอก Prompt แล้ว แต่ไม่สามารถเปิดเบราว์เซอร์/แอปได้", Toast.LENGTH_SHORT).show()
        }
    }
}

