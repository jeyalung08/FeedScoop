// FILE: ui/screens/CalibrationScreen.kt
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
fun CalibrationScreen(
    navController: NavController,
    deviceViewModel: DeviceViewModel = hiltViewModel()
) {
    val calibrationStatus by deviceViewModel.calibrationStatus.collectAsState()
    val isConnected       by deviceViewModel.isConnected.collectAsState()
    val currentWeight     by deviceViewModel.currentWeight.collectAsState()
    val deviceStatus      by deviceViewModel.deviceStatus.collectAsState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibration", color = Color(0xFF2E7D32)) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            // ── Device status ───────────────────────────────────────
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isConnected) "Device Online" else "Device Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF2E7D32) else Color.Red
                    )
                    if (isConnected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Live weight: ${String.format("%.3f", currentWeight)} kg",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Status: $deviceStatus",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }


            // ── Calibration status ──────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Calibration Status", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        calibrationStatus,
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            calibrationStatus.contains("Complete") -> Color(0xFF2E7D32)
                            calibrationStatus.contains("...") || calibrationStatus.contains("ing") -> Color(0xFFF57C00)
                            calibrationStatus.contains("Failed") -> Color.Red
                            else -> Color.Gray
                        }
                    )
                }
            }


            // ── Instructions ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Before Calibrating",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFF57C00)
                    )
                    Text(
                        "1. Remove all weight from the scoop and load cell.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "2. Place the scoop on a flat, stable surface.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "3. Make sure the device is online (green status above).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "4. Tap 'Tare Scale' — the ESP32 will zero the load cell.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Note: Calibration factor is set in firmware. " +
                                "Contact your developer to adjust if readings are inaccurate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }


            // ── Tare button ─────────────────────────────────────────
            Button(
                onClick = { deviceViewModel.startCalibration() },
                modifier = Modifier.fillMaxWidth(),
                enabled = isConnected,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Tare Scale (Zero)", color = Color.White)
            }


            if (!isConnected) {
                Text(
                    "Device must be online to tare. Check Device Connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }


            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Back") }
        }
    }
}
