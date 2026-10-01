package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.protocol.WifiSubnetInfo
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.NeutralBorder

@Composable
fun ScopeDialog(
    currentTarget: String,
    wifiSubnetInfo: WifiSubnetInfo,
    onSelectTarget: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember {
        mutableStateOf(
            when {
                currentTarget == wifiSubnetInfo.cidrSuggestion -> "subnet"
                currentTarget == "broadcast" -> "broadcast"
                else -> "custom"
            }
        )
    }
    var customText by remember {
        mutableStateOf(if (selectedOption == "custom") currentTarget else "192.168.1.0/24")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("scope_dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Select Discovery Target",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Option 1: Local Subnet
                if (wifiSubnetInfo.cidrSuggestion.isNotBlank()) {
                    ScopeOptionItem(
                        title = "Local Wi-Fi Subnet (Recommended)",
                        subtitle = wifiSubnetInfo.cidrSuggestion,
                        selected = selectedOption == "subnet",
                        onClick = { selectedOption = "subnet" }
                    )
                }

                // Option 2: Broadcast
                ScopeOptionItem(
                    title = "Broadcast Discovery",
                    subtitle = "255.255.255.255 & local broadcast",
                    selected = selectedOption == "broadcast",
                    onClick = { selectedOption = "broadcast" }
                )

                // Option 3: Custom
                ScopeOptionItem(
                    title = "Custom CIDR, Range, or IP",
                    subtitle = "e.g. 10.114.58.1-100 or 10.0.0.0/24",
                    selected = selectedOption == "custom",
                    onClick = { selectedOption = "custom" }
                )

                if (selectedOption == "custom") {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("Target IP, Range, or Subnet") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTarget = when (selectedOption) {
                        "subnet" -> wifiSubnetInfo.cidrSuggestion
                        "broadcast" -> "broadcast"
                        else -> customText.trim().ifEmpty { "broadcast" }
                    }
                    onSelectTarget(finalTarget)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ScopeOptionItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) BrandBlue else NeutralBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = BrandBlue)
            )
            Column(modifier = Modifier.padding(start = 6.dp)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
