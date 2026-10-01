package com.example.protocol

import java.net.InetAddress

object UbntTargetGenerator {

    /**
     * Generates a sequence of target IP address strings from the user input string.
     * Matches Python carrier-grade generator logic:
     * 1. Broadcast ("broadcast", "bcast", "all", "255.255.255.255")
     * 2. Short range (e.g. 10.114.58.1-100)
     * 3. Full range (e.g. 10.0.0.1-10.0.1.50)
     * 4. CIDR notation (e.g. 10.114.58.0/24)
     * 5. Single IP
     */
    fun generateTargets(targetInput: String, localBroadcastIp: String? = null): Sequence<String> = sequence {
        val target = targetInput.trim()
        if (target.isBlank()) return@sequence

        val lower = target.lowercase()
        // 1. Broadcast check
        if (lower in listOf("broadcast", "bcast", "all", "255.255.255.255")) {
            yield("255.255.255.255")
            // Also probe local subnet broadcast if available for best reliability on Android Wi-Fi
            if (!localBroadcastIp.isNullOrBlank() && localBroadcastIp != "255.255.255.255") {
                yield(localBroadcastIp)
            }
            return@sequence
        }

        // 2. Short range: e.g. 10.114.58.1-100
        val shortRangeRegex = Regex("""^(\d+\.\d+\.\d+\.)(\d+)-(\d+)$""")
        val shortMatch = shortRangeRegex.matchEntire(target)
        if (shortMatch != null) {
            val prefix = shortMatch.groupValues[1]
            val s = shortMatch.groupValues[2].toIntOrNull() ?: 1
            val e = shortMatch.groupValues[3].toIntOrNull() ?: 1
            val start = minOf(s, e).coerceIn(0, 255)
            val end = maxOf(s, e).coerceIn(0, 255)
            for (i in start..end) {
                yield("$prefix$i")
            }
            return@sequence
        }

        // 3. Full range: e.g. 10.0.0.1-10.0.1.50
        val fullRangeRegex = Regex("""^(\d+\.\d+\.\d+\.\d+)-(\d+\.\d+\.\d+\.\d+)$""")
        val fullMatch = fullRangeRegex.matchEntire(target)
        if (fullMatch != null) {
            val startIp = parseIpv4(fullMatch.groupValues[1])
            val endIp = parseIpv4(fullMatch.groupValues[2])
            if (startIp != null && endIp != null) {
                val start = minOf(startIp, endIp)
                val end = maxOf(startIp, endIp)
                val count = end - start + 1
                // Cap at 65536 hosts to avoid accidental massive loops
                val limit = count.coerceAtMost(65536)
                for (i in 0 until limit) {
                    yield(formatIpv4(start + i))
                }
                return@sequence
            }
        }

        // 4. CIDR notation: e.g. 10.114.58.0/24
        if (target.contains("/")) {
            val parts = target.split("/")
            if (parts.size == 2) {
                val baseIp = parseIpv4(parts[0].trim())
                val prefix = parts[1].trim().toIntOrNull()
                if (baseIp != null && prefix != null && prefix in 8..32) {
                    val mask = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
                    val network = baseIp and mask
                    val totalHosts = 1L shl (32 - prefix)

                    if (prefix >= 31) {
                        for (i in 0 until totalHosts) {
                            yield(formatIpv4(network + i))
                        }
                    } else {
                        // Scan usable hosts: network + 1 up to network + totalHosts - 2
                        val maxToYield = (totalHosts - 2).coerceAtMost(65534)
                        for (i in 1..maxToYield) {
                            yield(formatIpv4(network + i))
                        }
                    }
                    return@sequence
                }
            }
        }

        // 5. Single address
        yield(target)
    }

    fun parseIpv4(ipStr: String): Long? {
        return try {
            val bytes = InetAddress.getByName(ipStr.trim()).address
            if (bytes.size != 4) null
            else {
                ((bytes[0].toLong() and 0xFF) shl 24) or
                        ((bytes[1].toLong() and 0xFF) shl 16) or
                        ((bytes[2].toLong() and 0xFF) shl 8) or
                        (bytes[3].toLong() and 0xFF)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatIpv4(ipLong: Long): String {
        val b0 = (ipLong ushr 24) and 0xFF
        val b1 = (ipLong ushr 16) and 0xFF
        val b2 = (ipLong ushr 8) and 0xFF
        val b3 = ipLong and 0xFF
        return "$b0.$b1.$b2.$b3"
    }
}
