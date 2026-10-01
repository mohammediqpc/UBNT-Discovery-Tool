package com.example.protocol

import java.net.InetAddress

object UbntPacketParser {

    private val MODES_MAP = mapOf(
        0x01 to "Ad-Hoc",
        0x02 to "Station (Client)",
        0x03 to "Access Point (AP)",
        0x04 to "Repeater (AP-Repeater)",
        0x05 to "Secondary",
        0x06 to "Monitor"
    )

    /**
     * Parses raw UDP payload received from Ubiquiti devices according to the Carrier Grade Ubnt discovery format.
     */
    fun parse(data: ByteArray, fromIp: String = ""): UbntDeviceInfo? {
        if (data.size < 4) return null

        val length = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        val payloadEnd = (4 + length).coerceAtMost(data.size)
        if (payloadEnd <= 4) return null

        val payload = data.copyOfRange(4, payloadEnd)

        var deviceName = "N/A"
        var model = "N/A"
        var platform = "N/A"
        val macs = mutableListOf<String>()
        val ips = mutableListOf<String>()
        var firmware = "N/A"
        var ssid = "N/A"
        var uptimeSeconds: Long? = null
        var mode = "N/A"
        var webPort: Int? = null
        var bssid: String? = null
        var isDefault = false
        var seq: Long? = null
        val unknownTlvs = mutableListOf<UnknownTlv>()

        var offset = 0
        while (offset + 3 <= payload.size) {
            val t = payload[offset].toInt() and 0xFF
            val l = ((payload[offset + 1].toInt() and 0xFF) shl 8) or (payload[offset + 2].toInt() and 0xFF)
            offset += 3
            if (offset + l > payload.size) break
            val v = payload.copyOfRange(offset, offset + l)
            offset += l

            when (t) {
                0x01 -> { // MAC Address
                    val mac = formatMac(v)
                    if (mac.isNotEmpty() && !macs.contains(mac)) {
                        macs.add(mac)
                    }
                }
                0x02 -> { // MAC + IP Pair (10 bytes: 6 MAC + 4 IP)
                    if (v.size == 10) {
                        val mac = formatMac(v.copyOfRange(0, 6))
                        val ipBytes = v.copyOfRange(6, 10)
                        try {
                            val ip = InetAddress.getByAddress(ipBytes).hostAddress ?: ""
                            if (mac.isNotEmpty() && !macs.contains(mac)) macs.add(mac)
                            if (ip.isNotEmpty() && !ips.contains(ip)) ips.add(ip)
                        } catch (_: Exception) {
                        }
                    }
                }
                0x03 -> { // Firmware String
                    firmware = String(v, Charsets.UTF_8).trim()
                }
                0x0A -> { // Uptime in seconds (4 bytes unsigned int)
                    if (v.size == 4) {
                        uptimeSeconds = ((v[0].toLong() and 0xFF) shl 24) or
                                ((v[1].toLong() and 0xFF) shl 16) or
                                ((v[2].toLong() and 0xFF) shl 8) or
                                (v[3].toLong() and 0xFF)
                    }
                }
                0x0B -> { // Device Hostname
                    deviceName = String(v, Charsets.UTF_8).trim()
                }
                0x0C -> { // Platform / Short Model
                    platform = String(v, Charsets.UTF_8).trim()
                    if (model == "N/A") {
                        model = platform
                    }
                }
                0x0D -> { // Wireless SSID
                    ssid = String(v, Charsets.UTF_8).trim()
                }
                0x0E -> { // Wireless Mode
                    if (v.isNotEmpty()) {
                        val modeCode = v[0].toInt() and 0xFF
                        mode = MODES_MAP[modeCode] ?: "Unknown (0x%02X)".format(modeCode)
                    }
                }
                0x10 -> { // Web / Management Port (2 bytes unsigned short)
                    if (v.size == 2) {
                        webPort = ((v[0].toInt() and 0xFF) shl 8) or (v[1].toInt() and 0xFF)
                    }
                }
                0x12 -> { // BSSID / Connected MAC (6 bytes)
                    bssid = formatMac(v)
                }
                0x14 -> { // Full Model Name
                    model = String(v, Charsets.UTF_8).trim()
                }
                0x16 -> { // Factory Default Status
                    if (v.isNotEmpty() && (v[0].toInt() and 0xFF) == 1) {
                        isDefault = true
                    }
                }
                0x18 -> { // Sequence Number (4 bytes)
                    if (v.size == 4) {
                        seq = ((v[0].toLong() and 0xFF) shl 24) or
                                ((v[1].toLong() and 0xFF) shl 16) or
                                ((v[2].toLong() and 0xFF) shl 8) or
                                (v[3].toLong() and 0xFF)
                    }
                }
                else -> { // Carrier Grade zero-loss tracking of unknown TLVs
                    unknownTlvs.add(
                        UnknownTlv(
                            type = "0x%02X".format(t),
                            length = l,
                            hexValue = v.joinToString("") { "%02X".format(it) }
                        )
                    )
                }
            }
        }

        if (fromIp.isNotEmpty() && !ips.contains(fromIp)) {
            ips.add(fromIp)
        }

        return UbntDeviceInfo(
            deviceName = deviceName,
            model = model,
            platform = platform,
            macs = macs,
            ips = ips,
            firmware = firmware,
            ssid = ssid,
            uptimeSeconds = uptimeSeconds,
            mode = mode,
            webPort = webPort,
            bssid = bssid,
            isDefault = isDefault,
            seq = seq,
            unknownTlvs = unknownTlvs,
            discoveredFromIp = fromIp
        )
    }

    private fun formatMac(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }
}
