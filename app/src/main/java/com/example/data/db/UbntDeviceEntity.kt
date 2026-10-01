package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.protocol.UbntDeviceInfo
import com.example.protocol.UnknownTlv

@Entity(tableName = "ubnt_devices")
data class UbntDeviceEntity(
    @PrimaryKey
    val mac: String,
    val primaryIp: String,
    val allIpsJoined: String,
    val allMacsJoined: String,
    val deviceName: String,
    val model: String,
    val platform: String,
    val firmware: String,
    val ssid: String,
    val mode: String,
    val uptimeSeconds: Long?,
    val webPort: Int?,
    val bssid: String?,
    val isDefault: Boolean,
    val seq: Long?,
    val unknownTlvsJson: String,
    val discoveredFromIp: String,
    val lastSeenTimestamp: Long,
    val isFavorite: Boolean = false,
    val customNote: String = ""
) {
    fun toDomain(): UbntDeviceInfo {
        val macList = allMacsJoined.split(",").filter { it.isNotBlank() }
        val ipList = allIpsJoined.split(",").filter { it.isNotBlank() }
        val parsedUnknownTlvs = parseUnknownTlvs(unknownTlvsJson)

        return UbntDeviceInfo(
            deviceName = deviceName,
            model = model,
            platform = platform,
            macs = if (macList.isNotEmpty()) macList else listOf(mac),
            ips = if (ipList.isNotEmpty()) ipList else listOf(primaryIp),
            firmware = firmware,
            ssid = ssid,
            uptimeSeconds = uptimeSeconds,
            mode = mode,
            webPort = webPort,
            bssid = bssid,
            isDefault = isDefault,
            seq = seq,
            unknownTlvs = parsedUnknownTlvs,
            discoveredFromIp = discoveredFromIp,
            timestamp = lastSeenTimestamp
        )
    }

    companion object {
        fun fromDomain(info: UbntDeviceInfo, isFavorite: Boolean = false, note: String = ""): UbntDeviceEntity {
            val primaryMac = info.primaryMac
            val primaryIp = info.primaryIp
            val ipsJoined = info.ips.joinToString(",")
            val macsJoined = info.macs.joinToString(",")
            val unknownJson = serializeUnknownTlvs(info.unknownTlvs)

            return UbntDeviceEntity(
                mac = primaryMac,
                primaryIp = primaryIp,
                allIpsJoined = ipsJoined,
                allMacsJoined = macsJoined,
                deviceName = info.deviceName,
                model = info.model,
                platform = info.platform,
                firmware = info.firmware,
                ssid = info.ssid,
                mode = info.mode,
                uptimeSeconds = info.uptimeSeconds,
                webPort = info.webPort,
                bssid = info.bssid,
                isDefault = info.isDefault,
                seq = info.seq,
                unknownTlvsJson = unknownJson,
                discoveredFromIp = info.discoveredFromIp,
                lastSeenTimestamp = info.timestamp,
                isFavorite = isFavorite,
                customNote = note
            )
        }

        private fun serializeUnknownTlvs(list: List<UnknownTlv>): String {
            if (list.isEmpty()) return ""
            // Simple serialization format: type;len;hex|type;len;hex
            return list.joinToString("|") { "${it.type};${it.length};${it.hexValue}" }
        }

        private fun parseUnknownTlvs(raw: String): List<UnknownTlv> {
            if (raw.isBlank()) return emptyList()
            return try {
                raw.split("|").mapNotNull { item ->
                    val parts = item.split(";")
                    if (parts.size >= 3) {
                        UnknownTlv(type = parts[0], length = parts[1].toIntOrNull() ?: 0, hexValue = parts[2])
                    } else null
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
