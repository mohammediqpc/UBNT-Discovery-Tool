package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.UbntDeviceDao
import com.example.data.db.UbntDeviceEntity
import com.example.protocol.UbntDeviceInfo
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class UbntRepository(private val dao: UbntDeviceDao) {

    val allDevicesFlow: Flow<List<UbntDeviceEntity>> = dao.getAllDevicesFlow()

    suspend fun saveDevice(info: UbntDeviceInfo) {
        val existing = dao.getDeviceByMac(info.primaryMac)
        val isFav = existing?.isFavorite ?: false
        val note = existing?.customNote ?: ""
        dao.insertOrUpdate(UbntDeviceEntity.fromDomain(info, isFavorite = isFav, note = note))
    }

    suspend fun saveAll(devices: List<UbntDeviceInfo>) {
        val entities = devices.map { info ->
            val existing = dao.getDeviceByMac(info.primaryMac)
            val isFav = existing?.isFavorite ?: false
            val note = existing?.customNote ?: ""
            UbntDeviceEntity.fromDomain(info, isFavorite = isFav, note = note)
        }
        dao.insertOrUpdateAll(entities)
    }

    suspend fun toggleFavorite(mac: String, current: Boolean) {
        dao.setFavorite(mac, !current)
    }

    suspend fun updateNote(mac: String, note: String) {
        dao.updateNote(mac, note)
    }

    suspend fun deleteDevice(mac: String) {
        dao.deleteDevice(mac)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    /**
     * Serializes devices to JSON array, conforming to python script `--json` format:
     */
    fun exportToJson(devices: List<UbntDeviceInfo>): String {
        val array = JSONArray()
        for (dev in devices) {
            val obj = JSONObject()
            obj.put("device_name", dev.deviceName)
            obj.put("model", dev.model)
            obj.put("platform", dev.platform)

            val macsArray = JSONArray()
            dev.macs.forEach { macsArray.put(it) }
            obj.put("macs", macsArray)

            val ipsArray = JSONArray()
            dev.ips.forEach { ipsArray.put(it) }
            obj.put("ips", ipsArray)

            obj.put("firmware", dev.firmware)
            obj.put("ssid", dev.ssid)
            if (dev.uptimeSeconds != null) {
                obj.put("uptime", dev.uptimeSeconds)
            } else {
                obj.put("uptime", JSONObject.NULL)
            }
            obj.put("mode", dev.mode)
            if (dev.webPort != null) {
                obj.put("web_port", dev.webPort)
            } else {
                obj.put("web_port", JSONObject.NULL)
            }
            if (dev.bssid != null) {
                obj.put("bssid", dev.bssid)
            }
            obj.put("is_default", dev.isDefault)
            if (dev.seq != null) {
                obj.put("seq", dev.seq)
            }

            val unknownArray = JSONArray()
            for (u in dev.unknownTlvs) {
                val uObj = JSONObject()
                uObj.put("type", u.type)
                uObj.put("len", u.length)
                uObj.put("hex", u.hexValue)
                unknownArray.put(uObj)
            }
            obj.put("unknown_tlvs", unknownArray)

            array.put(obj)
        }
        return array.toString(2)
    }
}
