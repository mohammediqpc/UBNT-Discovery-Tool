package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.protocol.ScanConfig
import com.example.protocol.ScanProgress
import com.example.protocol.ScanState
import com.example.protocol.WifiSubnetInfo
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.NeutralSurfaceAlt
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ScanControlCard(
    targetInput: String,
    onTargetChange: (String) -> Unit,
    scanConfig: ScanConfig,
    scanProgress: ScanProgress,
    wifiSubnetInfo: WifiSubnetInfo,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning = scanProgress.state == ScanState.SCANNING || scanProgress.state == ScanState.DRAINING
    var showScopeDropdown by remember { mutableStateOf(false) }
    var isCustomTargetMode by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeutralBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Scope Description & Settings button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DISCOVERY SCOPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            targetInput == "broadcast" -> "Broadcast (255.255.255.255)"
                            targetInput == wifiSubnetInfo.cidrSuggestion -> "Local Subnet (${wifiSubnetInfo.cidrSuggestion})"
                            else -> "Target: $targetInput"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Scope Selector Menu Button
                    Box {
                        OutlinedButton(
                            onClick = { showScopeDropdown = true },
                            enabled = !isScanning,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Change", fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showScopeDropdown,
                            onDismissRequest = { showScopeDropdown = false }
                        ) {
                            if (wifiSubnetInfo.cidrSuggestion.isNotBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Local Wi-Fi Subnet (Recommended)", fontWeight = FontWeight.Medium)
                                            Text(wifiSubnetInfo.cidrSuggestion, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        onTargetChange(wifiSubnetInfo.cidrSuggestion)
                                        isCustomTargetMode = false
                                        showScopeDropdown = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("All Broadcast", fontWeight = FontWeight.Medium)
                                        Text("255.255.255.255 & local broadcast", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    onTargetChange("broadcast")
                                    isCustomTargetMode = false
                                    showScopeDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Custom IP / CIDR / Range", fontWeight = FontWeight.Medium)
                                        Text("Enter manual subnet or single IP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    isCustomTargetMode = true
                                    showScopeDropdown = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onOpenSettings,
                        enabled = !isScanning,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Scan Tuning",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Custom Target Input (expanded only if user clicked Custom or wants to edit)
            AnimatedVisibility(visible = isCustomTargetMode) {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = onTargetChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_input_field"),
                    label = { Text("Custom Target (CIDR, Range, or Single IP)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. 192.168.1.0/24 or 10.114.58.1-100") },
                    singleLine = true,
                    enabled = !isScanning,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        IconButton(onClick = { isCustomTargetMode = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }

            // Action Button: Full-width clear Start/Stop button
            if (isScanning) {
                Button(
                    onClick = onStopScan,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("stop_scan_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Discovery", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                Button(
                    onClick = onStartScan,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_scan_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Discovery Scan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Clean, non-distracting scan status / progress indicator
            AnimatedVisibility(visible = isScanning || scanProgress.state == ScanState.COMPLETED) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeutralSurfaceAlt)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (scanProgress.state) {
                                ScanState.SCANNING -> "Probing node ${scanProgress.currentTarget}..."
                                ScanState.DRAINING -> "Listening for delayed microwave/link replies..."
                                ScanState.COMPLETED -> "Discovery complete (${scanProgress.devicesFound} devices found)"
                                ScanState.CANCELLED -> "Scan stopped by user"
                                ScanState.ERROR -> "Scan failed: ${scanProgress.errorMessage}"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (scanProgress.state == ScanState.ERROR) StatusError else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${scanProgress.devicesFound} found",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (scanProgress.devicesFound > 0) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp)),
                            color = BrandBlue,
                            trackColor = NeutralBorder
                        )
                    }
                }
            }
        }
    }
}
