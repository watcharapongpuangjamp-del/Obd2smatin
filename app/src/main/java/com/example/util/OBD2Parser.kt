package com.example.util

import android.util.Log

/**
 * OBD2Parser สำหรับจัดการและคำนวณค่า Raw Hex จาก ELM327
 */
object OBD2Parser {
    private const val TAG = "OBD2Parser"

    /**
     * ตรวจสอบ Error พื้นฐานจาก ELM327
     * @return null หากไม่มี Error, คืนค่า String ข้อความ Error หากพบปัญหา
     */
    fun checkError(response: String): String? {
        val r = response.uppercase().trim()
        return when {
            r.contains("NO DATA") -> "NO DATA: ECU ไม่ตอบสนอง"
            r.contains("SEARCHING") -> "SEARCHING: กำลังค้นหาโปรโตคอล"
            r.contains("?") -> "UNKNOWN: คำสั่งไม่ถูกต้อง"
            r.contains("UNABLE TO CONNECT") -> "ERROR: เชื่อมต่อ ECU ไม่ได้"
            r.contains("STOPPED") -> "STOPPED: การสื่อสารหยุดชะงัก"
            r.isEmpty() -> "EMPTY: ไม่มีข้อมูลตอบกลับ"
            else -> null
        }
    }

    /**
     * ทำความสะอาด Response และแปลงเป็น List ของ Hex String
     * เช่น "41 0C 0B F4 >" -> ["41", "0C", "0B", "F4"]
     */
    private fun cleanResponse(response: String): List<String> {
        return response.replace(">", "")
            .replace("\r", " ")
            .replace("\n", " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
    }

    /**
     * 1. คำนวณ RPM (PID 010C)
     * Response Format: 41 0C AA BB
     * สูตร: ((A * 256) + B) / 4
     */
    fun parseRpm(response: String): Double {
        val error = checkError(response)
        if (error != null) throw Exception(error)

        val hexList = cleanResponse(response)
        // ค้นหาตำแหน่ง 41 0C
        val index = hexList.indexOf("0C")
        if (index != -1 && hexList.getOrNull(index - 1) == "41" && hexList.size > index + 2) {
            val a = hexList[index + 1].toInt(16)
            val b = hexList[index + 2].toInt(16)
            return ((a * 256.0) + b) / 4.0
        }
        throw Exception("Invalid RPM format: $response")
    }

    /**
     * 2. คำนวณ Speed (PID 010D)
     * Response Format: 41 0D AA
     * สูตร: A
     */
    fun parseSpeed(response: String): Int {
        val error = checkError(response)
        if (error != null) throw Exception(error)

        val hexList = cleanResponse(response)
        val index = hexList.indexOf("0D")
        if (index != -1 && hexList.getOrNull(index - 1) == "41" && hexList.size > index + 1) {
            return hexList[index + 1].toInt(16)
        }
        throw Exception("Invalid Speed format: $response")
    }

    /**
     * 3. คำนวณ Coolant Temperature (PID 0105)
     * Response Format: 41 05 AA
     * สูตร: A - 40
     */
    fun parseCoolantTemp(response: String): Int {
        checkError(response)?.let { throw Exception(it) }

        val hexList = cleanResponse(response)
        val index = hexList.indexOf("05")
        if (index != -1 && hexList.getOrNull(index - 1) == "41" && hexList.size > index + 1) {
            val a = hexList[index + 1].toInt(16)
            return a - 40
        }
        throw Exception("Invalid Coolant Temp format: $response")
    }

    /**
     * 4. คำนวณ Engine Load (PID 0104)
     * Response Format: 41 04 AA
     * สูตร: (A * 100) / 255
     */
    fun parseEngineLoad(response: String): Double {
        checkError(response)?.let { throw Exception(it) }

        val hexList = cleanResponse(response)
        val index = hexList.indexOf("04")
        if (index != -1 && hexList.getOrNull(index - 1) == "41" && hexList.size > index + 1) {
            val a = hexList[index + 1].toInt(16)
            return (a * 100.0) / 255.0
        }
        throw Exception("Invalid Engine Load format: $response")
    }

    /**
     * แปลงแรงดันไฟแบตเตอรี่ (AT RV)
     * Response เช่น "12.4V"
     */
    fun parseVoltage(response: String): String {
        return response.replace(">", "").trim()
    }
}
