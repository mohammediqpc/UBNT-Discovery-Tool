package com.example

import com.example.protocol.UbntPacketParser
import com.example.protocol.UbntTargetGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class ExampleUnitTest {

    @Test
    fun testTargetGeneratorBroadcast() {
        val targets = UbntTargetGenerator.generateTargets("broadcast", localBroadcastIp = "192.168.1.255").toList()
        assertTrue(targets.contains("255.255.255.255"))
        assertTrue(targets.contains("192.168.1.255"))
    }

    @Test
    fun testTargetGeneratorShortRange() {
        val targets = UbntTargetGenerator.generateTargets("10.114.58.1-5").toList()
        assertEquals(listOf("10.114.58.1", "10.114.58.2", "10.114.58.3", "10.114.58.4", "10.114.58.5"), targets)
    }

    @Test
    fun testTargetGeneratorFullRange() {
        val targets = UbntTargetGenerator.generateTargets("10.0.0.1-10.0.0.3").toList()
        assertEquals(listOf("10.0.0.1", "10.0.0.2", "10.0.0.3"), targets)
    }

    @Test
    fun testTargetGeneratorCidr() {
        val targets = UbntTargetGenerator.generateTargets("192.168.10.0/30").toList()
        // /30 has 2 usable host addresses: .1 and .2
        assertEquals(listOf("192.168.10.1", "192.168.10.2"), targets)
    }

    @Test
    fun testTargetGeneratorSingleIp() {
        val targets = UbntTargetGenerator.generateTargets("192.168.1.20").toList()
        assertEquals(listOf("192.168.1.20"), targets)
    }

    @Test
    fun testUbntPacketParser() {
        // Build a synthetic Ubiquiti Discovery response packet:
        // Header: [version=1, reserved=0, length=2 bytes]
        // TLV 0x01: MAC (6 bytes: AA:BB:CC:DD:EE:FF)
        // TLV 0x0B: Hostname (bytes: "Tower-Loco-5AC")
        // TLV 0x14: Full Model (bytes: "NanoStation 5AC Loco")
        // TLV 0x0E: Wireless Mode (1 byte: 0x03 -> Access Point (AP))
        // TLV 0x16: Factory Default (1 byte: 0x01 -> true)
        // TLV 0x0A: Uptime (4 bytes: 90061 seconds -> 1d 1h 1m 1s)
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Payload
        val payloadBaos = ByteArrayOutputStream()
        val payloadDos = DataOutputStream(payloadBaos)

        // TLV 0x01: MAC
        payloadDos.writeByte(0x01)
        payloadDos.writeShort(6)
        payloadDos.write(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()))

        // TLV 0x0B: Hostname
        val nameBytes = "Tower-Loco-5AC".toByteArray(Charsets.UTF_8)
        payloadDos.writeByte(0x0B)
        payloadDos.writeShort(nameBytes.size)
        payloadDos.write(nameBytes)

        // TLV 0x14: Model
        val modelBytes = "NanoStation 5AC Loco".toByteArray(Charsets.UTF_8)
        payloadDos.writeByte(0x14)
        payloadDos.writeShort(modelBytes.size)
        payloadDos.write(modelBytes)

        // TLV 0x0E: Mode
        payloadDos.writeByte(0x0E)
        payloadDos.writeShort(1)
        payloadDos.writeByte(0x03) // AP

        // TLV 0x16: Factory Default
        payloadDos.writeByte(0x16)
        payloadDos.writeShort(1)
        payloadDos.writeByte(0x01) // isDefault = true

        // TLV 0x0A: Uptime
        payloadDos.writeByte(0x0A)
        payloadDos.writeShort(4)
        payloadDos.writeInt(90061)

        val payload = payloadBaos.toByteArray()

        // Write header
        dos.writeByte(0x01) // header version
        dos.writeByte(0x00) // reserved
        dos.writeShort(payload.size) // length
        dos.write(payload)

        val packetData = baos.toByteArray()
        val parsed = UbntPacketParser.parse(packetData, fromIp = "192.168.1.20")

        assertNotNull(parsed)
        parsed!!
        assertEquals("Tower-Loco-5AC", parsed.deviceName)
        assertEquals("NanoStation 5AC Loco", parsed.model)
        assertEquals("Access Point (AP)", parsed.mode)
        assertTrue(parsed.isDefault)
        assertEquals(90061L, parsed.uptimeSeconds)
        assertEquals("AA:BB:CC:DD:EE:FF", parsed.primaryMac)
        assertEquals("192.168.1.20", parsed.primaryIp)
        assertEquals("1d 1h 1m 1s (90061s)", parsed.formattedUptime())
    }
}
