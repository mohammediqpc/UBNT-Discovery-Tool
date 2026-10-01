package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.UbntDeviceEntity
import com.example.data.repository.UbntRepository
import com.example.protocol.NetworkUtils
import com.example.protocol.ScanConfig
import com.example.protocol.ScanProgress
import com.example.protocol.ScanState
import com.example.protocol.UbntDeviceInfo
import com.example.protocol.UbntDiscoveryEngine
import com.example.protocol.WifiSubnetInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FilterOption {
    ALL,
    AP_ONLY,
    STATION_ONLY,
    DEFAULT_ONLY,
    FAVORITES_ONLY
}

enum class SortOption {
    IP,
    NAME,
    MODEL,
    UPTIME,
    LAST_SEEN
}

enum class ScreenTab {
    LIVE_SCAN,
    SAVED_HISTORY
}

private data class RawDevicesData(
    val devices: List<UbntDeviceInfo>,
    val favoriteMacs: Set<String>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UbntRepository
    private val engine: UbntDiscoveryEngine

    init {
        val db = AppDatabase.getDatabase(application)
        repository = UbntRepository(db.ubntDeviceDao())
        engine = UbntDiscoveryEngine(application)
    }

    private val _targetInput = MutableStateFlow("broadcast")
    val targetInput: StateFlow<String> = _targetInput.asStateFlow()

    private val _scanConfig = MutableStateFlow(ScanConfig())
    val scanConfig: StateFlow<ScanConfig> = _scanConfig.asStateFlow()

    private val _wifiSubnetInfo = MutableStateFlow(NetworkUtils.getWifiSubnetInfo(application))
    val wifiSubnetInfo: StateFlow<WifiSubnetInfo> = _wifiSubnetInfo.asStateFlow()

    private val _activeTab = MutableStateFlow(ScreenTab.LIVE_SCAN)
    val activeTab: StateFlow<ScreenTab> = _activeTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.IP)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _simulatedProgress = MutableStateFlow<ScanProgress?>(null)

    val scanProgress: StateFlow<ScanProgress> = combine(engine.progress, _simulatedProgress) { eng, sim ->
        sim ?: eng
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanProgress())

    private var simulationJob: kotlinx.coroutines.Job? = null

    val labDemoDevices = listOf(
        UbntDeviceInfo(
            deviceName = "Tower-East-Loco",
            model = "NanoStation 5AC Loco",
            platform = "WA",
            macs = listOf("00:27:22:9A:14:B2"),
            ips = listOf("192.168.1.20"),
            firmware = "WA.v8.7.1.44299.210419.1432",
            ssid = "Tower-North-Backhaul",
            uptimeSeconds = 3628800L,
            mode = "Station (Client)",
            webPort = 80,
            bssid = "00:27:22:8F:33:C1",
            isDefault = false,
            discoveredFromIp = "192.168.1.20"
        ),
        UbntDeviceInfo(
            deviceName = "Main-Sector-Prism",
            model = "Rocket Prism 5AC Gen2",
            platform = "XC",
            macs = listOf("00:27:22:8F:33:C1"),
            ips = listOf("192.168.1.1"),
            firmware = "XC.v8.7.1.44299.210419.1432",
            ssid = "Tower-North-Backhaul",
            uptimeSeconds = 10368000L,
            mode = "Access Point (AP)",
            webPort = 443,
            bssid = null,
            isDefault = false,
            discoveredFromIp = "192.168.1.1"
        ),
        UbntDeviceInfo(
            deviceName = "Core-PtP-Master",
            model = "airFiber 5XHD",
            platform = "AF09",
            macs = listOf("F0:9F:C2:10:44:EE"),
            ips = listOf("192.168.1.50"),
            firmware = "AF09.v1.5.0.33412.220110.1000",
            ssid = "Backbone-Fiber-10G",
            uptimeSeconds = 18144000L,
            mode = "Access Point (AP)",
            webPort = 443,
            bssid = null,
            isDefault = false,
            discoveredFromIp = "192.168.1.50"
        ),
        UbntDeviceInfo(
            deviceName = "South-Link-Dish",
            model = "PowerBeam 5AC Gen2",
            platform = "WA",
            macs = listOf("00:27:22:C4:65:88"),
            ips = listOf("192.168.1.35"),
            firmware = "WA.v8.7.11.45890.220915.1120",
            ssid = "Tower-South-Sector",
            uptimeSeconds = 1296000L,
            mode = "Station (Client)",
            webPort = 80,
            bssid = "00:27:22:8F:33:C1",
            isDefault = false,
            discoveredFromIp = "192.168.1.35"
        ),
        UbntDeviceInfo(
            deviceName = "HQ-Corporate-AP",
            model = "UniFi AP-AC-Pro",
            platform = "UAP",
            macs = listOf("B4:FB:E4:55:12:9A"),
            ips = listOf("192.168.1.10"),
            firmware = "6.5.62.14788",
            ssid = "Enterprise-Corp-5G",
            uptimeSeconds = 7603200L,
            mode = "Access Point (AP)",
            webPort = 80,
            bssid = null,
            isDefault = false,
            discoveredFromIp = "192.168.1.10"
        ),
        UbntDeviceInfo(
            deviceName = "New-Deploy-CPE",
            model = "LiteBeam 5AC Gen2",
            platform = "WA",
            macs = listOf("00:27:22:44:91:0F"),
            ips = listOf("192.168.1.21"),
            firmware = "WA.v8.7.1.44299.210419.1432",
            ssid = "ubnt",
            uptimeSeconds = 1800L,
            mode = "Station (Client)",
            webPort = 80,
            bssid = null,
            isDefault = true,
            discoveredFromIp = "192.168.1.21"
        ),
        UbntDeviceInfo(
            deviceName = "Site-Gateway-EdgeRouter",
            model = "EdgeRouter 4",
            platform = "ER-4",
            macs = listOf("80:2A:A8:CC:11:42"),
            ips = listOf("192.168.1.254"),
            firmware = "v2.0.9-hotfix.7",
            ssid = "N/A",
            uptimeSeconds = 25920000L,
            mode = "Gateway / Router",
            webPort = 443,
            bssid = null,
            isDefault = false,
            discoveredFromIp = "192.168.1.254"
        )
    )

    // Live session discovered devices
    private val _liveDevices = MutableStateFlow<Map<String, UbntDeviceInfo>>(emptyMap())

    // All saved devices from Room database
    val savedEntities: StateFlow<List<UbntDeviceEntity>> = repository.allDevicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected device for modal sheets
    private val _selectedDevice = MutableStateFlow<UbntDeviceInfo?>(null)
    val selectedDevice: StateFlow<UbntDeviceInfo?> = _selectedDevice.asStateFlow()

    private val _pingTarget = MutableStateFlow<UbntDeviceInfo?>(null)
    val pingTarget: StateFlow<UbntDeviceInfo?> = _pingTarget.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    init {
        refreshWifi()
    }

    fun refreshWifi() {
        val info = NetworkUtils.getWifiSubnetInfo(getApplication())
        _wifiSubnetInfo.value = info
    }

    fun setTargetInput(newTarget: String) {
        _targetInput.value = newTarget
    }

    fun updateScanConfig(config: ScanConfig) {
        _scanConfig.value = config
        _showSettings.value = false
    }

    fun openSettings() {
        _showSettings.value = true
    }

    fun closeSettings() {
        _showSettings.value = false
    }

    fun setActiveTab(tab: ScreenTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterOption(option: FilterOption) {
        _filterOption.value = option
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun selectDevice(device: UbntDeviceInfo?) {
        _selectedDevice.value = device
    }

    fun openPing(device: UbntDeviceInfo?) {
        _pingTarget.value = device
    }

    fun closePing() {
        _pingTarget.value = null
    }

    fun enableSimulationMode() {
        _isSimulationMode.value = true
        _liveDevices.value = labDemoDevices.associateBy { it.primaryMac }
        viewModelScope.launch {
            labDemoDevices.forEach { repository.saveDevice(it) }
        }
    }

    fun disableSimulationMode() {
        _isSimulationMode.value = false
        simulationJob?.cancel()
        simulationJob = null
        _simulatedProgress.value = null
        _liveDevices.value = emptyMap()
    }

    fun toggleSimulationMode() {
        if (_isSimulationMode.value) {
            disableSimulationMode()
        } else {
            enableSimulationMode()
        }
    }

    fun startScan() {
        val config = _scanConfig.value.copy(target = _targetInput.value)
        _scanConfig.value = config
        _liveDevices.value = emptyMap()

        if (_isSimulationMode.value) {
            simulationJob?.cancel()
            simulationJob = viewModelScope.launch {
                val total = labDemoDevices.size
                _simulatedProgress.value = ScanProgress(
                    state = ScanState.SCANNING,
                    targetsCount = total,
                    totalIps = total,
                    scannedIps = 0,
                    devicesFound = 0,
                    currentTarget = "Simulated Lab Subnet"
                )

                for (i in 0 until total) {
                    kotlinx.coroutines.delay(350)
                    val dev = labDemoDevices[i]
                    val current = _liveDevices.value.toMutableMap()
                    current[dev.primaryMac] = dev
                    _liveDevices.value = current

                    repository.saveDevice(dev)

                    _simulatedProgress.value = ScanProgress(
                        state = ScanState.SCANNING,
                        targetsCount = total,
                        totalIps = total,
                        scannedIps = i + 1,
                        devicesFound = i + 1,
                        currentTarget = dev.primaryIp,
                        elapsedSeconds = (i + 1).toLong()
                    )
                }

                kotlinx.coroutines.delay(200)
                _simulatedProgress.value = ScanProgress(
                    state = ScanState.COMPLETED,
                    targetsCount = total,
                    totalIps = total,
                    scannedIps = total,
                    devicesFound = total,
                    elapsedSeconds = total.toLong()
                )
            }
            return
        }

        _simulatedProgress.value = null
        engine.startScan(config, viewModelScope) { device ->
            val current = _liveDevices.value.toMutableMap()
            val key = device.primaryMac
            current[key] = device
            _liveDevices.value = current

            // Auto persist to Room database
            viewModelScope.launch {
                repository.saveDevice(device)
            }
        }
    }

    fun stopScan() {
        simulationJob?.cancel()
        simulationJob = null
        if (_simulatedProgress.value?.state == ScanState.SCANNING) {
            _simulatedProgress.value = _simulatedProgress.value?.copy(state = ScanState.CANCELLED)
        }
        engine.stopScan()
    }

    fun toggleFavorite(mac: String, current: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(mac, current)
        }
    }

    fun updateNote(mac: String, note: String) {
        viewModelScope.launch {
            repository.updateNote(mac, note)
        }
    }

    fun deleteDevice(mac: String) {
        viewModelScope.launch {
            repository.deleteDevice(mac)
            val current = _liveDevices.value.toMutableMap()
            current.remove(mac)
            _liveDevices.value = current
            if (_selectedDevice.value?.primaryMac == mac) {
                _selectedDevice.value = null
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _liveDevices.value = emptyMap()
            _selectedDevice.value = null
        }
    }

    // Step 1: Combine active tab, live devices and saved entities into RawDevicesData
    private val rawDevicesFlow = combine(_activeTab, _liveDevices, savedEntities) { tab, liveMap, savedList ->
        val list = if (tab == ScreenTab.LIVE_SCAN) {
            liveMap.values.toList()
        } else {
            savedList.map { it.toDomain() }
        }
        val favs = savedList.filter { it.isFavorite }.map { it.mac }.toSet()
        RawDevicesData(list, favs)
    }

    // Step 2: Combine with search, filter, and sort
    val displayedDevices: StateFlow<List<UbntDeviceInfo>> = combine(
        rawDevicesFlow,
        _searchQuery,
        _filterOption,
        _sortOption
    ) { rawData, query, filter, sort ->
        val rawList = rawData.devices
        val favoriteMacs = rawData.favoriteMacs

        // 1. Search filter
        val searchFiltered = if (query.isBlank()) {
            rawList
        } else {
            val q = query.trim().lowercase()
            rawList.filter { dev ->
                dev.deviceName.lowercase().contains(q) ||
                        dev.model.lowercase().contains(q) ||
                        dev.platform.lowercase().contains(q) ||
                        dev.primaryIp.contains(q) ||
                        dev.primaryMac.lowercase().contains(q) ||
                        dev.ssid.lowercase().contains(q) ||
                        dev.firmware.lowercase().contains(q)
            }
        }

        // 2. Category filter
        val categoryFiltered = when (filter) {
            FilterOption.ALL -> searchFiltered
            FilterOption.AP_ONLY -> searchFiltered.filter { it.mode.contains("AP", ignoreCase = true) }
            FilterOption.STATION_ONLY -> searchFiltered.filter { it.mode.contains("Station", ignoreCase = true) }
            FilterOption.DEFAULT_ONLY -> searchFiltered.filter { it.isDefault }
            FilterOption.FAVORITES_ONLY -> searchFiltered.filter { favoriteMacs.contains(it.primaryMac) }
        }

        // 3. Sorting
        when (sort) {
            SortOption.IP -> categoryFiltered.sortedBy { dev ->
                dev.ips.firstOrNull() ?: dev.discoveredFromIp
            }
            SortOption.NAME -> categoryFiltered.sortedBy { it.displayTitle.lowercase() }
            SortOption.MODEL -> categoryFiltered.sortedBy { it.model.lowercase() }
            SortOption.UPTIME -> categoryFiltered.sortedByDescending { it.uptimeSeconds ?: -1L }
            SortOption.LAST_SEEN -> categoryFiltered.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportCurrentJson(): String {
        return repository.exportToJson(displayedDevices.value)
    }
}
