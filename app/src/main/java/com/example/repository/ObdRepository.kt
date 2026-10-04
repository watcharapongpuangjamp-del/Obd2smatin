package com.example.repository

import android.hardware.usb.UsbDevice
import android.util.Log
import com.example.model.ConnectionStatus
import com.example.model.ObdDataState
import com.example.model.ObdProtocol
import com.example.util.OBD2Parser
import com.example.util.UsbSerialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ObdRepository(private val usbManager: UsbSerialManager) {

    private val _obdState = MutableStateFlow(ObdDataState())
    val obdState: StateFlow<ObdDataState> = _obdState.asStateFlow()

    private var isRunning = false

    suspend fun connect(device: UsbDevice, protocol: ObdProtocol = ObdProtocol.AUTO): Boolean = withContext(Dispatchers.IO) {
        _obdState.value = _obdState.value.copy(connectionStatus = ConnectionStatus.CONNECTING)
        
        if (usbManager.connect(device)) {
            try {
                // 1. Initialize Adapter
                sendCommand("AT Z")   // Reset
                delay(500)
                sendCommand("AT E0")  // Echo Off
                sendCommand("AT L0")  // Linefeeds Off
                
                // 2. Set Protocol
                sendCommand(protocol.command)
                
                // 3. Check Protocol
                val protoResp = sendCommand("AT DP")
                val protoName = protoResp.replace(">", "").trim()
                
                _obdState.value = _obdState.value.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    currentProtocol = protoName
                )
                startDataStream()
                return@withContext true
            } catch (e: Exception) {
                Log.e("ObdRepository", "Init Error: ${e.message}")
                _obdState.value = _obdState.value.copy(
                    connectionStatus = ConnectionStatus.ERROR,
                    lastError = e.message
                )
            }
        } else {
            _obdState.value = _obdState.value.copy(connectionStatus = ConnectionStatus.ERROR, lastError = "Failed to open USB port")
        }
        false
    }

    private suspend fun sendCommand(cmd: String): String {
        usbManager.write("$cmd\r")
        return usbManager.read()
    }

    private fun startDataStream() {
        if (isRunning) return
        isRunning = true
        // In a real app, use a dedicated loop in a Coroutine
    }

    suspend fun updateLiveData() = withContext(Dispatchers.IO) {
        if (_obdState.value.connectionStatus != ConnectionStatus.CONNECTED) return@withContext

        try {
            val rpmRaw = sendCommand("010C")
            val rpm = try { OBD2Parser.parseRpm(rpmRaw) } catch (e: Exception) { 0.0 }

            val speedRaw = sendCommand("010D")
            val speed = try { OBD2Parser.parseSpeed(speedRaw) } catch (e: Exception) { 0 }

            val tempRaw = sendCommand("0105")
            val temp = try { OBD2Parser.parseCoolantTemp(tempRaw) } catch (e: Exception) { 0 }

            val loadRaw = sendCommand("0104")
            val load = try { OBD2Parser.parseEngineLoad(loadRaw) } catch (e: Exception) { 0.0 }

            val voltRaw = sendCommand("AT RV")
            val volt = OBD2Parser.parseVoltage(voltRaw)

            _obdState.value = _obdState.value.copy(
                rpm = rpm,
                speed = speed,
                coolantTemp = temp,
                engineLoad = load,
                voltage = volt
            )
        } catch (e: Exception) {
            Log.e("ObdRepository", "Stream Error: ${e.message}")
        }
    }

    suspend fun scanDtcs() = withContext(Dispatchers.IO) {
        try {
            val resp = sendCommand("03")
            val codes = try {
                OBD2Parser.parseDtcList(resp)
            } catch (e: Exception) {
                emptyList()
            }
            _obdState.value = _obdState.value.copy(dtcs = codes)
        } catch (e: Exception) {
            Log.e("ObdRepository", "DTC Error: ${e.message}")
        }
    }

    suspend fun clearDtcs() = withContext(Dispatchers.IO) {
        sendCommand("04")
        _obdState.value = _obdState.value.copy(dtcs = emptyList())
    }

    suspend fun resetAdapter() = withContext(Dispatchers.IO) {
        sendCommand("AT WS") // Warm Start
        delay(500)
        sendCommand("AT Z")  // Full Reset
        delay(1000)
        _obdState.value = _obdState.value.copy(connectionStatus = ConnectionStatus.DISCONNECTED)
    }

    fun disconnect() {
        isRunning = false
        usbManager.disconnect()
        _obdState.value = _obdState.value.copy(connectionStatus = ConnectionStatus.DISCONNECTED)
    }

    fun setSelectedProtocol(protocol: ObdProtocol) {
        _obdState.value = _obdState.value.copy(selectedProtocol = protocol)
    }
}
