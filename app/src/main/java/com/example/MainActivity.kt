package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.protocol.ScanState
import com.example.protocol.UbntDeviceInfo
import com.example.ui.components.DeviceCard
import com.example.ui.components.DeviceDetailSheet
import com.example.ui.components.PingDialog
import com.example.ui.components.ScopeDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeutralBg
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.NeutralSurfaceAlt
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.FilterOption
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.SortOption
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure high-contrast dark status bar icons on Light theme
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            MyApplicationTheme(darkTheme = false) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val targetInput by viewModel.targetInput.collectAsStateWithLifecycle()
    val scanConfig by viewModel.scanConfig.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val wifiInfo by viewModel.wifiSubnetInfo.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterOption by viewModel.filterOption.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val displayedDevices by viewModel.displayedDevices.collectAsStateWithLifecycle()
    val savedEntities by viewModel.savedEntities.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val pingTarget by viewModel.pingTarget.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val isSimulationMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()

    var showScopeDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val isScanning = scanProgress.state == ScanState.SCANNING || scanProgress.state == ScanState.DRAINING

    fun copyToClipboard(text: String, label: String = "Copied") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        scope.launch {
            snackbarHostState.showSnackbar("$label copied: $text")
        }
    }

    fun openWebConsole(device: UbntDeviceInfo) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(device.webUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open browser for ${device.webUrl}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareExport(jsonText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "Ubiquiti Discovered Nodes")
            putExtra(Intent.EXTRA_TEXT, jsonText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Discovered Nodes"))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NeutralBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Radar,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Ubiquiti Discovery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (wifiInfo.isConnected) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (wifiInfo.isConnected) "${wifiInfo.ssid}${if (wifiInfo.localIp.isNotEmpty()) " (${wifiInfo.localIp})" else ""}" else "Offline / No Network",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Lab Simulation Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleSimulationMode() },
                        modifier = Modifier.testTag("simulation_mode_toggle_button")
                    ) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = "Simulation Lab Mode",
                            tint = if (isSimulationMode) StatusWarning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sort menu
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by IP Address") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.IP)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Device Name") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NAME)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Model") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.MODEL)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Uptime") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.UPTIME)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // Export JSON button
                    IconButton(
                        onClick = {
                            val json = viewModel.exportCurrentJson()
                            shareExport(json)
                        },
                        modifier = Modifier.testTag("export_json_button")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share JSON",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Clear Devices
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("clear_all_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear List",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Official Bottom Navigation Bar for effortless UX and accessibility
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                // 1. Live Discovery Tab
                NavigationBarItem(
                    selected = activeTab == ScreenTab.LIVE_SCAN,
                    onClick = { viewModel.setActiveTab(ScreenTab.LIVE_SCAN) },
                    icon = {
                        if (displayedDevices.isNotEmpty() && activeTab == ScreenTab.LIVE_SCAN) {
                            BadgedBox(badge = { Badge { Text("${displayedDevices.size}") } }) {
                                Icon(Icons.Default.Radar, contentDescription = "Live Scan")
                            }
                        } else {
                            Icon(Icons.Default.Radar, contentDescription = "Live Scan")
                        }
                    },
                    label = { Text("Live Scan") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBlue,
                        indicatorColor = BrandBlueLight
                    )
                )

                // 2. Saved History Tab
                NavigationBarItem(
                    selected = activeTab == ScreenTab.SAVED_HISTORY,
                    onClick = { viewModel.setActiveTab(ScreenTab.SAVED_HISTORY) },
                    icon = {
                        if (savedEntities.isNotEmpty()) {
                            BadgedBox(badge = { Badge { Text("${savedEntities.size}") } }) {
                                Icon(Icons.Default.History, contentDescription = "History")
                            }
                        } else {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    },
                    label = { Text("Saved History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBlue,
                        indicatorColor = BrandBlueLight
                    )
                )

                // 3. Settings / Tuning Tab
                NavigationBarItem(
                    selected = showSettings,
                    onClick = { viewModel.openSettings() },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBlue,
                        indicatorColor = BrandBlueLight
                    )
                )
            }
        },
        floatingActionButton = {
            if (activeTab == ScreenTab.LIVE_SCAN) {
                if (isScanning) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.stopScan() },
                        icon = { Icon(Icons.Default.Stop, contentDescription = null) },
                        text = { Text("Stop Scan", fontWeight = FontWeight.Bold) },
                        containerColor = StatusError,
                        contentColor = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.testTag("stop_scan_fab")
                    )
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.startScan() },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        text = { Text("Scan Network", fontWeight = FontWeight.Bold) },
                        containerColor = BrandBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("start_scan_fab")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Target Selector & Discovery Scope Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showScopeDialog = true }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "Scope: ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                targetInput == "broadcast" -> "Broadcast"
                                targetInput == wifiInfo.cidrSuggestion -> "Subnet (${wifiInfo.cidrSuggestion})"
                                else -> targetInput
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• Change",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = "${displayedDevices.size} Devices",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Lab Simulation Mode Banner (when active)
            AnimatedVisibility(visible = isSimulationMode) {
                Surface(
                    color = androidx.compose.ui.graphics.Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lab Simulation Active (7 Demo Nodes)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF92400E)
                            )
                        }
                        TextButton(
                            onClick = { viewModel.disableSimulationMode() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Exit Demo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            // Detailed Scan Progress Indicator (Shown while scanning)
            AnimatedVisibility(visible = isScanning) {
                Surface(
                    color = BrandBlueLight,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (scanProgress.state == ScanState.DRAINING)
                                    "Awaiting delayed links..."
                                else
                                    "Probing: ${scanProgress.currentTarget}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandBlue
                            )
                            val minutes = scanProgress.elapsedSeconds / 60
                            val seconds = scanProgress.elapsedSeconds % 60
                            Text(
                                text = "Elapsed: %02d:%02ds".format(minutes, seconds),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Progress fraction
                        if (scanProgress.totalIps > 0) {
                            LinearProgressIndicator(
                                progress = { scanProgress.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = BrandBlue,
                                trackColor = NeutralBorder
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "IPs: ${scanProgress.scannedIps} / ${scanProgress.totalIps} (${(scanProgress.progressFraction * 100).toInt()}%)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Found: ${scanProgress.devicesFound}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSuccess
                                )
                            }
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = BrandBlue
                            )
                        }
                    }
                }
            }

            // Compact Search Bar (Placeholder fully visible and not cut off)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(52.dp)
                    .testTag("search_devices_input"),
                placeholder = {
                    Text(
                        text = "Search by IP, MAC, Model, Name...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = NeutralBorder,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Category Filter Chips (Default Config removed as requested)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterOption == FilterOption.ALL,
                    onClick = { viewModel.setFilterOption(FilterOption.ALL) },
                    label = { Text("All (${displayedDevices.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlueLight,
                        selectedLabelColor = BrandBlue
                    )
                )

                FilterChip(
                    selected = filterOption == FilterOption.AP_ONLY,
                    onClick = { viewModel.setFilterOption(FilterOption.AP_ONLY) },
                    label = { Text("Access Points", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(13.dp))
                    }
                )

                FilterChip(
                    selected = filterOption == FilterOption.STATION_ONLY,
                    onClick = { viewModel.setFilterOption(FilterOption.STATION_ONLY) },
                    label = { Text("Stations", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(13.dp))
                    }
                )

                FilterChip(
                    selected = filterOption == FilterOption.FAVORITES_ONLY,
                    onClick = { viewModel.setFilterOption(FilterOption.FAVORITES_ONLY) },
                    label = { Text("Starred", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(13.dp))
                    }
                )
            }

            // Discovered Device Cards (Maximized viewport)
            if (displayedDevices.isEmpty()) {
                EmptyStateView(
                    activeTab = activeTab,
                    isScanning = isScanning,
                    isSimulationMode = isSimulationMode,
                    onStartScan = { viewModel.startScan() },
                    onLoadDemoDevices = { viewModel.enableSimulationMode() }
                )
            } else {
                val favoriteMacs = savedEntities.filter { it.isFavorite }.map { it.mac }.toSet()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("device_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = displayedDevices,
                        key = { it.primaryMac }
                    ) { dev ->
                        val isFav = favoriteMacs.contains(dev.primaryMac)
                        DeviceCard(
                            device = dev,
                            isFavorite = isFav,
                            onDeviceClick = { viewModel.selectDevice(dev) },
                            onOpenWeb = { openWebConsole(dev) },
                            onPing = { viewModel.openPing(dev) },
                            onCopyIp = { copyToClipboard(dev.primaryIp, "IP Address") },
                            onToggleFavorite = { viewModel.toggleFavorite(dev.primaryMac, isFav) }
                        )
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet (With Multiple IP Badges and Tap-to-Copy)
    selectedDevice?.let { dev ->
        val entity = savedEntities.firstOrNull { it.mac == dev.primaryMac }
        val isFav = entity?.isFavorite ?: false
        val note = entity?.customNote ?: ""

        DeviceDetailSheet(
            device = dev,
            isFavorite = isFav,
            note = note,
            onDismiss = { viewModel.selectDevice(null) },
            onOpenWeb = { openWebConsole(dev) },
            onPing = { viewModel.openPing(dev) },
            onCopyJson = {
                val json = viewModel.exportCurrentJson()
                copyToClipboard(json, "Device JSON")
            },
            onShare = {
                val json = viewModel.exportCurrentJson()
                shareExport(json)
            },
            onToggleFavorite = { viewModel.toggleFavorite(dev.primaryMac, isFav) },
            onSaveNote = { newNote -> viewModel.updateNote(dev.primaryMac, newNote) },
            onCopyIp = { ip -> copyToClipboard(ip, "IP Address") }
        )
    }

    // Ping Diagnostic Dialog
    pingTarget?.let { dev ->
        PingDialog(
            targetIp = dev.primaryIp,
            port = 80,
            isSimulated = isSimulationMode,
            onDismiss = { viewModel.closePing() }
        )
    }

    // Scope Selection Dialog
    if (showScopeDialog) {
        ScopeDialog(
            currentTarget = targetInput,
            wifiSubnetInfo = wifiInfo,
            onSelectTarget = { newTarget -> viewModel.setTargetInput(newTarget) },
            onDismiss = { showScopeDialog = false }
        )
    }

    // Engine Tuning Settings Dialog
    if (showSettings) {
        SettingsDialog(
            currentConfig = scanConfig,
            onSave = { updated -> viewModel.updateScanConfig(updated) },
            onDismiss = { viewModel.closeSettings() }
        )
    }

    // Clear Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            title = { Text("Clear Devices", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear discovered devices and scan history?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyStateView(
    activeTab: ScreenTab,
    isScanning: Boolean,
    isSimulationMode: Boolean,
    onStartScan: () -> Unit,
    onLoadDemoDevices: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(BrandBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activeTab == ScreenTab.LIVE_SCAN) Icons.Default.Radar else Icons.Default.History,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = if (isScanning)
                    "Scanning Network..."
                else if (activeTab == ScreenTab.LIVE_SCAN)
                    "No Devices Discovered"
                else
                    "No Saved History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isScanning)
                    "Sending discovery probes and listening for node replies on port 10001..."
                else if (activeTab == ScreenTab.LIVE_SCAN)
                    "Ensure your phone is on the same local network as your Ubiquiti radios.\n\nIn the browser preview or demo environment, tap below to load simulated lab devices."
                else
                    "Devices discovered during live discovery will automatically be saved and indexed here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (!isScanning && activeTab == ScreenTab.LIVE_SCAN) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onStartScan,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Network", fontWeight = FontWeight.SemiBold)
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = onLoadDemoDevices,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load Lab Demo Devices (تجربة)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
