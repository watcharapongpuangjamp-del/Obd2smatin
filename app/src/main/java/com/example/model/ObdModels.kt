package com.example.model

data class ObdDataState(
    val rpm: Double = 0.0,
    val speed: Int = 0,
    val coolantTemp: Int = 0,
    val engineLoad: Double = 0.0,
    val voltage: String = "0.0V",
    val dtcs: List<String> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val currentProtocol: String = "Unknown",
    val lastError: String? = null
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class ObdProtocol(val command: String, val description: String) {
    AUTO("AT SP 0", "Automatic Detection"),
    SAE_J1850_PWM("AT SP 1", "SAE J1850 PWM"),
    SAE_J1850_VPW("AT SP 2", "SAE J1850 VPW"),
    ISO_9141_2("AT SP 3", "ISO 9141-2"),
    ISO_14230_4_KWP_5BAUD("AT SP 4", "ISO 14230-4 KWP (5 Baud)"),
    ISO_14230_4_KWP_FAST("AT SP 5", "ISO 14230-4 KWP (Fast Init)"),
    ISO_15765_4_CAN_11_500("AT SP 6", "ISO 15765-4 CAN (11bit 500k)"),
    ISO_15765_4_CAN_29_500("AT SP 7", "ISO 15765-4 CAN (29bit 500k)"),
    ISO_15765_4_CAN_11_250("AT SP 8", "ISO 15765-4 CAN (11bit 250k)"),
    ISO_15765_4_CAN_29_250("AT SP 9", "ISO 15765-4 CAN (29bit 250k)"),
    SAE_J1939_CAN("AT SP A", "SAE J1939 CAN")
}
