package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class UsbSerialManager(private val context: Context) {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbSerialPort: UsbSerialPort? = null

    companion object {
        private const val TAG = "UsbSerialManager"
        private const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
        private const val READ_WAIT_MILLIS = 200
        private const val WRITE_WAIT_MILLIS = 500
    }

    /**
     * 1. ค้นหาอุปกรณ์ USB Serial ที่เชื่อมต่ออยู่
     */
    fun findDevice(): UsbDevice? {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "No USB Serial devices found")
            return null
        }
        // เลือก Driver ตัวแรกที่พบ
        val driver = availableDrivers[0]
        return driver.device
    }

    /**
     * 2. ขอสิทธิ์ USB Permission จาก User
     */
    fun requestPermission(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val permissionIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                Intent(ACTION_USB_PERMISSION), 
                flags
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    /**
     * 3. เริ่มการเชื่อมต่อและตั้งค่าพารามิเตอร์ (38400, 8N1)
     */
    suspend fun connect(device: UsbDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            val connection = usbManager.openDevice(driver.device)
            if (connection == null) {
                Log.e(TAG, "Failed to open connection")
                return@withContext false
            }

            val port = driver.ports[0] // เลือกพอร์ตแรก
            port.open(connection)
            
            // ตั้งค่า 38400, 8 bits, 1 stop bit, no parity
            port.setParameters(38400, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            
            usbSerialPort = port
            Log.d(TAG, "USB Connected successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during connection: ${e.message}")
            false
        }
    }

    /**
     * 4. ฟังก์ชันเขียนข้อมูลไปยัง ELM327 (Async)
     */
    suspend fun write(data: String): Boolean = withContext(Dispatchers.IO) {
        val port = usbSerialPort ?: return@withContext false
        try {
            val bytes = data.toByteArray()
            port.write(bytes, WRITE_WAIT_MILLIS)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Write error: ${e.message}")
            false
        }
    }

    /**
     * 4. ฟังก์ชันอ่านข้อมูลจาก ELM327 (Async Polling)
     */
    suspend fun read(): String = withContext(Dispatchers.IO) {
        val port = usbSerialPort ?: return@withContext ""
        val buffer = ByteArray(1024)
        val result = StringBuilder()
        
        try {
            // อ่านวนซ้ำจนกว่าจะเจอเครื่องหมาย '>' (Prompt ของ ELM327)
            while (true) {
                val len = port.read(buffer, READ_WAIT_MILLIS)
                if (len > 0) {
                    val received = String(buffer, 0, len)
                    result.append(received)
                    if (received.contains(">")) break
                } else {
                    // ถ้าไม่มีข้อมูลเข้ามา พักสักครู่แล้วลองใหม่
                    delay(50)
                }
                
                // ป้องกัน Infinite Loop (ถ้าอ่านค้างเกิน 5 วินาที)
                // (สามารถปรับแต่ง Timeout ได้ตามต้องการ)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read error: ${e.message}")
        }
        
        result.toString()
    }

    fun disconnect() {
        try {
            usbSerialPort?.close()
            usbSerialPort = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing port: ${e.message}")
        }
    }
}
