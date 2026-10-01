package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.protocol.NetworkUtils
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.NeutralSurfaceAlt
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PingResult(
    val seq: Int,
    val success: Boolean,
    val timeMs: Long,
    val target: String
)

@Composable
fun PingDialog(
    targetIp: String,
    port: Int = 80,
    isSimulated: Boolean = false,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pingHistory = remember { mutableStateListOf<PingResult>() }
    var isPinging by remember { mutableStateOf(false) }

    fun runPingSession() {
        if (isPinging) return
        isPinging = true
        pingHistory.clear()

        scope.launch {
            for (i in 1..5) {
                val (success, time) = NetworkUtils.pingHost(targetIp, port = port, timeoutMs = 1200, isSimulated = isSimulated)
                pingHistory.add(PingResult(seq = i, success = success, timeMs = time, target = targetIp))
                if (i < 5) delay(250)
            }
            isPinging = false
        }
    }

    LaunchedEffect(targetIp) {
        runPingSession()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ping_dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Latency & Reachability Test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sending probes to $targetIp (port $port)...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = NeutralSurfaceAlt,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pingHistory.isEmpty() && isPinging) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BrandBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting to host...", fontSize = 13.sp)
                            }
                        }

                        pingHistory.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (p.success) StatusSuccess else StatusError)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Probe #${p.seq}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = if (p.success) "${p.timeMs} ms" else "Request Timeout",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (p.success) MaterialTheme.colorScheme.primary else StatusError
                                )
                            }
                        }

                        if (!isPinging && pingHistory.isNotEmpty()) {
                            val successes = pingHistory.filter { it.success }
                            val lossPercent = ((pingHistory.size - successes.size) * 100) / pingHistory.size
                            val avgTime = if (successes.isNotEmpty()) successes.map { it.timeMs }.average().toLong() else 0

                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.HorizontalDivider(color = NeutralBorder, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Transmission: ${pingHistory.size} sent, ${successes.size} received (${lossPercent}% packet loss)",
                                fontSize = 11.sp,
                                color = if (lossPercent == 0) StatusSuccess else StatusError,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (successes.isNotEmpty()) {
                                Text(
                                    text = "Round-Trip Average: $avgTime ms",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { runPingSession() },
                enabled = !isPinging,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
