package com.example.viewmodel

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ConnectionStatus
import com.example.model.ObdDataState
import com.example.model.ObdProtocol
import com.example.repository.ObdRepository
import com.example.util.UsbSerialManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ObdViewModel(application: Application) : AndroidViewModel(application) {

    private val usbManager = UsbSerialManager(application)
    private val repository = ObdRepository(usbManager)
    
    val obdState: StateFlow<ObdDataState> = repository.obdState

    private var dataJob: Job? = null

    fun scanForDevice(): UsbDevice? {
        return usbManager.findDevice()
    }

    fun requestPermission(device: UsbDevice) {
        usbManager.requestPermission(device)
    }

    fun connect(device: UsbDevice) {
        viewModelScope.launch {
            if (repository.connect(device, obdState.value.selectedProtocol)) {
                startPolling()
            }
        }
    }

    fun setSelectedProtocol(protocol: ObdProtocol) {
        repository.setSelectedProtocol(protocol)
    }

    private fun startPolling() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            var errorCount = 0
            while (obdState.value.connectionStatus == ConnectionStatus.CONNECTED) {
                try {
                    repository.updateLiveData()
                    errorCount = 0
                    delay(100) // Poll every 100ms (10Hz)
                } catch (e: Exception) {
                    errorCount++
                    if (errorCount > 3) {
                        // Attempt auto-reconnect
                        val device = scanForDevice()
                        if (device != null) {
                            connect(device)
                        }
                        break
                    }
                    delay(500)
                }
            }
        }
    }

    fun scanDtcs() {
        viewModelScope.launch {
            repository.scanDtcs()
        }
    }

    fun clearDtcs() {
        viewModelScope.launch {
            repository.clearDtcs()
        }
    }

    fun resetAdapter() {
        viewModelScope.launch {
            repository.resetAdapter()
            dataJob?.cancel()
        }
    }

    fun disconnect() {
        repository.disconnect()
        dataJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
