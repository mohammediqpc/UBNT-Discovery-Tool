package com.example.protocol

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

class UbntDiscoveryEngine(private val context: Context) {

    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private val _discoveredDevices = MutableSharedFlow<UbntDeviceInfo>(replay = 100)
    val discoveredDevices: SharedFlow<UbntDeviceInfo> = _discoveredDevices.asSharedFlow()

    private var scanJob: Job? = null
    private val discoveredMap = ConcurrentHashMap<String, UbntDeviceInfo>()

    fun startScan(
        config: ScanConfig,
        scope: CoroutineScope,
        onDeviceFound: (UbntDeviceInfo) -> Unit = {}
    ) {
        stopScan()
        discoveredMap.clear()

        scanJob = scope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var socket: DatagramSocket? = null
            var multicastLock: WifiManager.MulticastLock? = null

            try {
                // Acquire Wi-Fi Multicast lock to ensure broadcast packets are not filtered by Android
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifiManager?.createMulticastLock("UBNT_DISCOVERY_LOCK")?.apply {
                    setReferenceCounted(true)
                    try {
                        acquire()
                    } catch (_: Exception) {
                    }
                }

                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    try {
                        receiveBufferSize = 2 * 1024 * 1024 // 2MB buffer for carrier-grade stability
                    } catch (_: Exception) {
                    }
                    bind(InetSocketAddress(0))
                    soTimeout = 100 // Non-blocking read timeout
                }

                _progress.value = ScanProgress(
                    state = ScanState.SCANNING,
                    targetsCount = 0,
                    probesSent = 0,
                    devicesFound = 0,
                    currentTarget = config.target
                )

                val wifiInfo = NetworkUtils.getWifiSubnetInfo(context)
                val targetList = UbntTargetGenerator.generateTargets(
                    targetInput = config.target,
                    localBroadcastIp = wifiInfo.broadcastIp
                ).toList()

                val totalTargets = targetList.size
                var probesSent = 0
                val probePayload = byteArrayOf(0x01, 0x00, 0x00, 0x00)

                // Receiver helper
                fun drainIncomingPackets(sock: DatagramSocket) {
                    val buffer = ByteArray(4096)
                    val packet = DatagramPacket(buffer, buffer.size)
                    while (true) {
                        try {
                            sock.receive(packet)
                            val fromIp = packet.address?.hostAddress ?: ""
                            val data = buffer.copyOf(packet.length)
                            val info = UbntPacketParser.parse(data, fromIp)
                            if (info != null) {
                                val key = info.macs.firstOrNull() ?: fromIp
                                if (!discoveredMap.containsKey(key)) {
                                    discoveredMap[key] = info
                                    onDeviceFound(info)
                                    _discoveredDevices.tryEmit(info)
                                    _progress.value = _progress.value.copy(
                                        devicesFound = discoveredMap.size,
                                        durationMs = System.currentTimeMillis() - startTime
                                    )
                                }
                            }
                        } catch (_: SocketTimeoutException) {
                            break
                        } catch (e: Exception) {
                            break
                        }
                    }
                }

                // 1. Transmission phase
                var scannedIps = 0
                for (targetIp in targetList) {
                    if (!isActive) break
                    scannedIps++
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000

                    try {
                        val inetAddr = InetAddress.getByName(targetIp)
                        val outPacket = DatagramPacket(probePayload, probePayload.size, inetAddr, config.port)
                        for (r in 0 until config.retries) {
                            try {
                                socket.send(outPacket)
                                probesSent++
                            } catch (_: Exception) {
                            }
                        }
                    } catch (_: Exception) {
                    }

                    // Intermittent receive drain
                    drainIncomingPackets(socket)

                    _progress.value = _progress.value.copy(
                        targetsCount = totalTargets,
                        totalIps = totalTargets,
                        scannedIps = scannedIps,
                        elapsedSeconds = elapsed,
                        probesSent = probesSent,
                        currentTarget = targetIp,
                        devicesFound = discoveredMap.size,
                        durationMs = System.currentTimeMillis() - startTime
                    )

                    if (config.delayMs > 0) {
                        delay(config.delayMs)
                    }
                }

                // 2. High-Latency Link Drain phase
                if (isActive) {
                    val drainStart = System.currentTimeMillis()
                    val drainDurationMs = (config.timeoutSeconds * 1000).toLong()

                    _progress.value = _progress.value.copy(
                        state = ScanState.DRAINING,
                        scannedIps = totalTargets,
                        totalIps = totalTargets,
                        elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000,
                        durationMs = System.currentTimeMillis() - startTime
                    )

                    while (isActive && (System.currentTimeMillis() - drainStart < drainDurationMs)) {
                        drainIncomingPackets(socket)
                        _progress.value = _progress.value.copy(
                            durationMs = System.currentTimeMillis() - startTime,
                            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000,
                            devicesFound = discoveredMap.size
                        )
                        delay(50)
                    }
                }

                _progress.value = _progress.value.copy(
                    state = if (isActive) ScanState.COMPLETED else ScanState.CANCELLED,
                    scannedIps = totalTargets,
                    totalIps = totalTargets,
                    elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000,
                    durationMs = System.currentTimeMillis() - startTime,
                    devicesFound = discoveredMap.size
                )

            } catch (e: CancellationException) {
                _progress.value = _progress.value.copy(
                    state = ScanState.CANCELLED,
                    durationMs = System.currentTimeMillis() - startTime
                )
            } catch (e: Exception) {
                _progress.value = _progress.value.copy(
                    state = ScanState.ERROR,
                    errorMessage = e.localizedMessage ?: "Unknown scanning error",
                    durationMs = System.currentTimeMillis() - startTime
                )
            } finally {
                withContext(Dispatchers.IO) {
                    try {
                        socket?.close()
                    } catch (_: Exception) {
                    }
                    try {
                        if (multicastLock?.isHeld == true) {
                            multicastLock.release()
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        if (_progress.value.state == ScanState.SCANNING || _progress.value.state == ScanState.DRAINING) {
            _progress.value = _progress.value.copy(state = ScanState.CANCELLED)
        }
    }

    fun getDiscoveredDevicesList(): List<UbntDeviceInfo> {
        return discoveredMap.values.toList()
    }
}
