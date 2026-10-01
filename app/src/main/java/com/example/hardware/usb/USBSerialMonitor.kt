package com.example.hardware.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.example.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Event emitted by USBSerialMonitor for UI banners, toasts, snackbars or animations
 */
sealed class UsbConnectionUiEvent {
    data class DevicePluggedIn(val deviceName: String, val chipType: String) : UsbConnectionUiEvent()
    data class DeviceUnplugged(val deviceName: String) : UsbConnectionUiEvent()
    data class PermissionPromptRequired(val deviceName: String) : UsbConnectionUiEvent()
    data class VehicleConnected(val protocol: String = "ISO 15765-4 CAN") : UsbConnectionUiEvent()
    data class VehicleDisconnected(val reason: String) : UsbConnectionUiEvent()
    data class ConnectionError(val errorMessage: String) : UsbConnectionUiEvent()
}

/**
 * High-level monitor that observes the USB OTG & OBD-II hardware connection state,
 * providing reactive StateFlows and single-shot UI events for rich visual feedback.
 */
class USBSerialMonitor(
    private val context: Context,
    private val permissionManager: UsbPermissionManager = UsbPermissionManager(context)
) {
    companion object {
        private const val TAG = "USBSerialMonitor"
    }

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var connectionStateJob: Job? = null

    // Reactive Connection State
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Attached USB Device (if any)
    private val _connectedDevice = MutableStateFlow<UsbDevice?>(null)
    val connectedDevice: StateFlow<UsbDevice?> = _connectedDevice.asStateFlow()

    // Chipset label (FTDI, CH340, CP2102, Prolific, etc.)
    private val _detectedChipset = MutableStateFlow<String?>("Unknown / Generic")
    val detectedChipset: StateFlow<String?> = _detectedChipset.asStateFlow()

    // One-shot UI visual notification events
    private val _uiEvents = MutableSharedFlow<UsbConnectionUiEvent>(extraBufferCapacity = 32)
    val uiEvents: SharedFlow<UsbConnectionUiEvent> = _uiEvents.asSharedFlow()

    private var previousState: ConnectionState = ConnectionState.DISCONNECTED

    /**
     * Start observing USB attach/detach and permission events.
     */
    fun startMonitoring() {
        Log.i(TAG, "Starting USBSerialMonitor...")
        permissionManager.startListening()

        if (observerJob == null || observerJob?.isActive == false) {
            observerJob = monitorScope.launch {
                permissionManager.deviceEvents.collect { event ->
                    handleDeviceEvent(event)
                }
            }
        }

        // Initial device check
        val initialDevice = permissionManager.scanForAttachedDevices()
        if (initialDevice != null) {
            _connectedDevice.value = initialDevice
            _detectedChipset.value = identifyChipset(initialDevice)
            Log.i(TAG, "Initial device detected on startup: ${initialDevice.deviceName} [${_detectedChipset.value}]")
        }
    }

    /**
     * Bind to UsbObdDriver's internal state flow to translate low-level adapter & ECU states into UI feedback.
     */
    fun bindDriverConnectionState(driverStateFlow: StateFlow<ConnectionState>) {
        connectionStateJob?.cancel()
        connectionStateJob = monitorScope.launch {
            driverStateFlow.collect { state ->
                updateConnectionState(state)
            }
        }
    }

    fun updateConnectionState(newState: ConnectionState) {
        val oldState = previousState
        previousState = newState
        _connectionState.value = newState

        Log.d(TAG, "USB Connection State Transition: $oldState -> $newState")

        when (newState) {
            ConnectionState.CONNECTED -> {
                _uiEvents.tryEmit(UsbConnectionUiEvent.VehicleConnected())
            }
            ConnectionState.DEVICE_DISCONNECTED -> {
                val devName = _connectedDevice.value?.deviceName ?: "OBD-II Adapter"
                _connectedDevice.value = null
                _detectedChipset.value = null
                _uiEvents.tryEmit(UsbConnectionUiEvent.DeviceUnplugged(devName))
                _uiEvents.tryEmit(UsbConnectionUiEvent.VehicleDisconnected("สาย USB ถูกถอดออก"))
            }
            ConnectionState.DISCONNECTED -> {
                if (oldState == ConnectionState.CONNECTED || oldState.ordinal >= ConnectionState.USB_OPEN.ordinal) {
                    _uiEvents.tryEmit(UsbConnectionUiEvent.VehicleDisconnected("ตัดการเชื่อมต่อเรียบร้อย"))
                }
            }
            ConnectionState.PERMISSION_REQUIRED -> {
                val devName = _connectedDevice.value?.deviceName ?: "USB Device"
                _uiEvents.tryEmit(UsbConnectionUiEvent.PermissionPromptRequired(devName))
            }
            ConnectionState.PERMISSION_DENIED -> {
                _uiEvents.tryEmit(UsbConnectionUiEvent.ConnectionError("ผู้ใช้ปฏิเสธการให้สิทธิ์เข้าถึง USB"))
            }
            ConnectionState.USB_OPEN_FAILED -> {
                _uiEvents.tryEmit(UsbConnectionUiEvent.ConnectionError("ไม่สามารถเปิดพอร์ตสื่อสาร USB ได้"))
            }
            ConnectionState.ECU_NOT_RESPONDING -> {
                _uiEvents.tryEmit(UsbConnectionUiEvent.ConnectionError("กล่อง ECU ไม่ตอบสนอง กรุณาตรวจสอบสวิตช์กุญแจรถยนต์"))
            }
            ConnectionState.ADAPTER_NOT_RESPONDING -> {
                _uiEvents.tryEmit(UsbConnectionUiEvent.ConnectionError("อะแดปเตอร์ ELM327 ไม่ตอบสนองคำสั่ง"))
            }
            ConnectionState.DISCONNECTED_STALE -> {
                _uiEvents.tryEmit(UsbConnectionUiEvent.ConnectionError("สัญญาณ USB ขาดหาย (Buffer Stale >500ms)"))
            }
            else -> {
                if (newState.isError) {
                    _uiEvents.tryEmit(UsbConnectionUiEvent.ConnectionError(newState.labelTh))
                }
            }
        }
    }

    private fun handleDeviceEvent(event: UsbDeviceEvent) {
        when (event) {
            is UsbDeviceEvent.Attached -> {
                _connectedDevice.value = event.device
                val chip = identifyChipset(event.device)
                _detectedChipset.value = chip
                Log.i(TAG, "USB Attached: ${event.device.deviceName}, Chip: $chip")
                _uiEvents.tryEmit(UsbConnectionUiEvent.DevicePluggedIn(event.device.deviceName, chip))
            }
            is UsbDeviceEvent.Detached -> {
                val devName = event.device.deviceName
                if (_connectedDevice.value?.deviceId == event.device.deviceId) {
                    _connectedDevice.value = null
                    _detectedChipset.value = null
                }
                Log.i(TAG, "USB Detached: $devName")
                _uiEvents.tryEmit(UsbConnectionUiEvent.DeviceUnplugged(devName))
            }
            is UsbDeviceEvent.PermissionResult -> {
                Log.i(TAG, "USB Permission Result: ${event.device.deviceName}, Granted: ${event.isGranted}")
                if (!event.isGranted) {
                    _uiEvents.tryEmit(UsbConnectionUiEvent.ConnectionError("ผู้ใช้ปฏิเสธการให้สิทธิ์เข้าถึงอุปกรณ์ ${event.device.deviceName}"))
                }
            }
        }
    }

    private fun identifyChipset(device: UsbDevice): String {
        val vid = device.vendorId
        val pid = device.productId

        return when {
            vid == 0x0403 -> "FTDI FT232R/FT232RL"
            vid == 0x1A86 -> "WCH CH340 / CH341"
            vid == 0x10C4 -> "Silicon Labs CP2102 / CP2104"
            vid == 0x067B -> "Prolific PL2303"
            else -> "USB UART (VID: 0x${vid.toString(16).uppercase()}, PID: 0x${pid.toString(16).uppercase()})"
        }
    }

    fun stopMonitoring() {
        Log.i(TAG, "Stopping USBSerialMonitor...")
        permissionManager.stopListening()
        observerJob?.cancel()
        observerJob = null
        connectionStateJob?.cancel()
        connectionStateJob = null
    }
}
