package com.example.protocol

/**
 * Data structures representing Ubiquiti Discovery Protocol items and device models.
 */

data class UnknownTlv(
    val type: String,
    val length: Int,
    val hexValue: String
)

data class UbntDeviceInfo(
    val deviceName: String = "N/A",
    val model: String = "N/A",
    val platform: String = "N/A",
    val macs: List<String> = emptyList(),
    val ips: List<String> = emptyList(),
    val firmware: String = "N/A",
    val ssid: String = "N/A",
    val uptimeSeconds: Long? = null,
    val mode: String = "N/A",
    val webPort: Int? = null,
    val bssid: String? = null,
    val isDefault: Boolean = false,
    val seq: Long? = null,
    val unknownTlvs: List<UnknownTlv> = emptyList(),
    val discoveredFromIp: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val primaryIp: String
        get() = ips.firstOrNull() ?: discoveredFromIp

    val primaryMac: String
        get() = macs.firstOrNull() ?: "00:00:00:00:00:00"

    fun formattedUptime(): String {
        val sec = uptimeSeconds ?: return "N/A"
        val days = sec / 86400
        val rem1 = sec % 86400
        val hours = rem1 / 3600
        val rem2 = rem1 % 3600
        val mins = rem2 / 60
        val s = rem2 % 60
        return "${days}d ${hours}h ${mins}m ${s}s (${sec}s)"
    }

    val displayTitle: String
        get() = if (deviceName.isNotBlank() && deviceName != "N/A") deviceName else model

    fun shortFirmware(): String {
        if (firmware.isBlank() || firmware == "N/A") return "N/A"
        val regex = Regex("""v?(\d+\.\d+(\.\d+)?)""")
        val match = regex.find(firmware)
        return if (match != null) "v${match.groupValues[1]}" else firmware.take(12)
    }

    val webUrl: String
        get() = "http://$primaryIp"
}

data class ScanConfig(
    val target: String = "broadcast",
    val retries: Int = 2,
    val timeoutSeconds: Double = 3.0,
    val delayMs: Long = 2L,
    val port: Int = 10001
)

enum class ScanState {
    IDLE,
    SCANNING,
    DRAINING,
    COMPLETED,
    CANCELLED,
    ERROR
}

data class ScanProgress(
    val state: ScanState = ScanState.IDLE,
    val targetsCount: Int = 0,
    val probesSent: Int = 0,
    val devicesFound: Int = 0,
    val currentTarget: String = "",
    val errorMessage: String? = null,
    val durationMs: Long = 0L,
    val totalIps: Int = 0,
    val scannedIps: Int = 0,
    val elapsedSeconds: Long = 0L
) {
    val progressFraction: Float
        get() = if (totalIps > 0) (scannedIps.toFloat() / totalIps.toFloat()).coerceIn(0f, 1f) else 0f
}
