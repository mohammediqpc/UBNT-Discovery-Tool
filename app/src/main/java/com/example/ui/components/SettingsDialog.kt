package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.protocol.ScanConfig
import com.example.ui.theme.BrandBlue

@Composable
fun SettingsDialog(
    currentConfig: ScanConfig,
    onSave: (ScanConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var retries by remember { mutableIntStateOf(currentConfig.retries) }
    var timeoutSeconds by remember { mutableDoubleStateOf(currentConfig.timeoutSeconds) }
    var delayMs by remember { mutableLongStateOf(currentConfig.delayMs) }
    var port by remember { mutableIntStateOf(currentConfig.port) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("settings_dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Engine & Transmission Tuning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure low-level UDP probe behavior for lossy wireless links and high-latency backhauls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Probes Per Host
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Probes per Host (Retries)", style = MaterialTheme.typography.bodyMedium)
                        Text("$retries probe(s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                    Slider(
                        value = retries.toFloat(),
                        onValueChange = { retries = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = BrandBlue, activeTrackColor = BrandBlue)
                    )
                    Text(
                        "Number of discovery packets sent per IP (Default: 2 for lossy links)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // 2. High-Latency Drain Timeout
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("High-Latency Drain Time", style = MaterialTheme.typography.bodyMedium)
                        Text("${"%.1f".format(timeoutSeconds)} s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                    Slider(
                        value = timeoutSeconds.toFloat(),
                        onValueChange = { timeoutSeconds = (Math.round(it * 2) / 2.0) },
                        valueRange = 1.0f..10.0f,
                        colors = SliderDefaults.colors(thumbColor = BrandBlue, activeTrackColor = BrandBlue)
                    )
                    Text(
                        "Time to wait listening for delayed replies after last probe (Default: 3.0s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // 3. Inter-Probe Delay
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Inter-Probe Delay", style = MaterialTheme.typography.bodyMedium)
                        Text("$delayMs ms", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                    Slider(
                        value = delayMs.toFloat(),
                        onValueChange = { delayMs = it.toLong() },
                        valueRange = 0f..20f,
                        colors = SliderDefaults.colors(thumbColor = BrandBlue, activeTrackColor = BrandBlue)
                    )
                    Text(
                        "Delay between packets to prevent switch/AP queue flooding (Default: 2ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // 4. UDP Port
                OutlinedTextField(
                    value = port.toString(),
                    onValueChange = { port = it.toIntOrNull() ?: 10001 },
                    label = { Text("Discovery UDP Port") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentConfig.copy(
                            retries = retries,
                            timeoutSeconds = timeoutSeconds,
                            delayMs = delayMs,
                            port = port
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Settings")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    retries = 2
                    timeoutSeconds = 3.0
                    delayMs = 2L
                    port = 10001
                }
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Defaults")
            }
        }
    )
}
