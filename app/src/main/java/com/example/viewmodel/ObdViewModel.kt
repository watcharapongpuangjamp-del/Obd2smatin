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

    fun connect(device: UsbDevice, protocol: ObdProtocol = ObdProtocol.AUTO) {
        viewModelScope.launch {
            if (repository.connect(device, protocol)) {
                startPolling()
            }
        }
    }

    private fun startPolling() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            while (obdState.value.connectionStatus == ConnectionStatus.CONNECTED) {
                repository.updateLiveData()
                delay(200) // Poll every 200ms
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
