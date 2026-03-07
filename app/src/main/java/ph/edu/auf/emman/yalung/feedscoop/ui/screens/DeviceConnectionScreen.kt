// FILE: ui/screens/DeviceConnectionScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConnectionScreen(
    navController: NavController,
    deviceViewModel: DeviceViewModel = hiltViewModel()
) {
    val isConnected     by deviceViewModel.isConnected.collectAsState()
    val deviceStatus    by deviceViewModel.deviceStatus.collectAsState()
    val currentWeight   by deviceViewModel.currentWeight.collectAsState()
    val cumulativeWeight by deviceViewModel.cumulativeWeight.collectAsState()

    val statusColor = when {
        isConnected && deviceStatus != "IDLE" -> Color(0xFF2E7D32)
        isConnected                           -> Color(0xFF1565C0)
        else                                  -> Color(0xFFC62828)
    }
    val statusLabel = when {
        !isConnected         -> "Offline — No data from device"
        deviceStatus == "IDLE"           -> "Online — Device idle"
        deviceStatus == "MEASURING"      -> "Online — Measuring"
        deviceStatus == "WAIT_DISPENSE"  -> "Online — Waiting for confirmation"
        deviceStatus == "COMPLETE"       -> "Online — Order complete"
        deviceStatus == "OVERWEIGHT"     -> "Online — Overweight!"
        else                             -> "Online — $deviceStatus"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Connection", color = Color(0xFF2E7D32)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Connection status card ──────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isConnected) "● Connected" else "● Disconnected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }
            }

            // ── Architecture info ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "How it works",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        "The ScoopSense prototype connects to the internet via WiFi " +
                                "and sends live weight data to Firebase. This app reads that " +
                                "data in real time — no direct Bluetooth pairing needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ConnectionRow("App → Firebase", "Writes target weight & order commands")
                    ConnectionRow("ESP32 → Firebase", "Writes live weight every 300ms")
                    ConnectionRow("App ← Firebase", "Reads live weight in real time")
                }
            }

            // ── Live data preview (only when connected) ─────────────
            if (isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Live Data",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF2E7D32)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current scoop:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${String.format("%.3f", currentWeight)} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cumulative:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${String.format("%.3f", cumulativeWeight)} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Device status:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                deviceStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Button(
                    onClick = { navController.navigate("calibration") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("Go to Calibration", color = Color.White) }
            } else {
                // Not connected — show troubleshooting guide
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Troubleshooting",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFF57C00)
                        )
                        Text("1. Make sure the ESP32 is powered on", style = MaterialTheme.typography.bodySmall)
                        Text("2. Check that the ESP32 is connected to WiFi", style = MaterialTheme.typography.bodySmall)
                        Text("3. Verify WiFi credentials in firmware", style = MaterialTheme.typography.bodySmall)
                        Text("4. Confirm Firebase rules allow read/write", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionRow(from: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "→",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Bold
        )
        Column {
            Text(from, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}