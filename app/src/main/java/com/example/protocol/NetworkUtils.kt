package com.example.protocol

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.system.measureTimeMillis

data class WifiSubnetInfo(
    val isConnected: Boolean,
    val ssid: String = "",
    val localIp: String = "",
    val broadcastIp: String = "255.255.255.255",
    val cidrSuggestion: String = "192.168.1.0/24"
)

object NetworkUtils {

    fun getWifiSubnetInfo(context: Context): WifiSubnetInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiInfo = wifiManager?.connectionInfo

        var localIp = ""
        var broadcastIp = "255.255.255.255"
        var cidr = "192.168.1.0/24"

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            // Prioritize Wi-Fi and Ethernet interfaces over cellular or virtual tunnel
            val prioritized = interfaces.sortedWith(compareByDescending { iface ->
                val name = iface.name.lowercase()
                when {
                    name.startsWith("wlan") -> 4
                    name.startsWith("eth") -> 3
                    name.startsWith("en") -> 3
                    !iface.isLoopback && iface.isUp -> 1
                    else -> 0
                }
            })

            for (iface in prioritized) {
                if (iface.isLoopback || !iface.isUp) continue

                for (addr in iface.interfaceAddresses) {
                    val ip = addr.address
                    if (ip is java.net.Inet4Address && !ip.isLoopbackAddress) {
                        localIp = ip.hostAddress ?: ""
                        val bcast = addr.broadcast
                        if (bcast != null) {
                            broadcastIp = bcast.hostAddress ?: "255.255.255.255"
                        }
                        val prefix = addr.networkPrefixLength
                        if (prefix > 0) {
                            val ipLong = UbntTargetGenerator.parseIpv4(localIp)
                            if (ipLong != null) {
                                val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
                                val netBase = ipLong and mask
                                cidr = "${UbntTargetGenerator.formatIpv4(netBase)}/$prefix"
                            }
                        }
                        break
                    }
                }
                if (localIp.isNotEmpty()) break
            }
        } catch (_: Exception) {
        }

        val rawSsid = wifiInfo?.ssid?.replace("\"", "") ?: ""
        val ssid = when {
            isWifi && rawSsid.isNotEmpty() && rawSsid != "<unknown ssid>" -> rawSsid
            isWifi -> "Wi-Fi Network"
            isEthernet -> "Ethernet / Emulator LAN"
            isCellular -> "Cellular Data"
            localIp.isNotEmpty() -> "Local LAN"
            hasInternet -> "Connected Network"
            else -> "Offline / No Connection"
        }

        val isConnected = hasInternet || isWifi || isEthernet || isCellular || localIp.isNotEmpty()

        return WifiSubnetInfo(
            isConnected = isConnected,
            ssid = ssid,
            localIp = localIp,
            broadcastIp = broadcastIp,
            cidrSuggestion = cidr
        )
    }

    suspend fun pingHost(
        ipStr: String,
        port: Int = 80,
        timeoutMs: Int = 1500,
        isSimulated: Boolean = false
    ): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        if (isSimulated) {
            val simulatedRtt = (2L..8L).random()
            kotlinx.coroutines.delay(simulatedRtt * 15)
            return@withContext true to simulatedRtt
        }

        val start = System.currentTimeMillis()
        try {
            // First try ICMP / standard isReachable
            val addr = InetAddress.getByName(ipStr)
            if (addr.isReachable(timeoutMs)) {
                val time = (System.currentTimeMillis() - start).coerceAtLeast(1)
                return@withContext true to time
            }
        } catch (_: Exception) {
        }

        // Fallback: TCP socket handshake to web port (or port 80 / 443 / 22)
        val socket = Socket()
        return@withContext try {
            val time = measureTimeMillis {
                socket.connect(InetSocketAddress(ipStr, port), timeoutMs)
            }
            socket.close()
            true to time.coerceAtLeast(1)
        } catch (_: Exception) {
            try {
                // Secondary fallback to port 443
                val socketSsl = Socket()
                val sslTime = measureTimeMillis {
                    socketSsl.connect(InetSocketAddress(ipStr, 443), timeoutMs)
                }
                socketSsl.close()
                true to sslTime.coerceAtLeast(1)
            } catch (_: Exception) {
                false to -1L
            }
        }
    }
}
