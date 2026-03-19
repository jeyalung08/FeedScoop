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
    val deviceStatus      by deviceViewModel.deviceStatus.collectAsState()

    // currentWeight now reflects actual scale reading in ALL states including IDLE
    // because the firmware was updated to write timerWeight (real reading)
    // instead of 0.0 during IDLE. This makes the calibration display accurate.
    val currentWeight by deviceViewModel.currentWeight.collectAsState()

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

            // ── Device online / offline ─────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
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
                        text       = if (isConnected) "Device Online" else "Device Offline",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (isConnected) Color(0xFF2E7D32) else Color.Red
                    )
                    if (isConnected) {
                        Text(
                            "Status: $deviceStatus",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            "Make sure the ESP32 is powered on and connected to WiFi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }

            // ── Live scale reading ──────────────────────────────────
            // This reads from /device/live/currentWeight which the firmware
            // now updates with the real scale value even when status is IDLE.
            // This means you can see the actual weight during calibration.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Live Scale Reading",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text       = "${String.format("%.3f", currentWeight)} kg",
                        style      = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color      = if (isConnected) Color(0xFF2E7D32) else Color.LightGray
                    )
                    Text(
                        text  = if (isConnected) "Updates every ~300ms from device"
                        else "No data — device offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // ── Tare status ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tare Status", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        calibrationStatus,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            calibrationStatus.contains("sent") ||
                                    calibrationStatus.contains("Complete") -> Color(0xFF2E7D32)
                            calibrationStatus.contains("Sending")  -> Color(0xFFF57C00)
                            calibrationStatus.contains("Failed")   -> Color.Red
                            else                                   -> Color.Gray
                        }
                    )
                }
            }

            // ── Instructions ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "How to Tare",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFF57C00)
                    )
                    Text("1. Remove all weight from the scoop.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("2. Watch the Live Scale Reading above stabilize near 0.000.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("3. Tap Tare Scale — reading should return to exactly 0.000.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("4. If it doesn't return to 0.000, tap Tare again.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Note: Calibration factor is set in firmware (currently 638.23). " +
                                "Contact your developer to adjust if readings are inaccurate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // ── Tare button ─────────────────────────────────────────
            Button(
                onClick  = { deviceViewModel.startCalibration() },
                modifier = Modifier.fillMaxWidth(),
                enabled  = isConnected,
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
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
                onClick  = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Back") }
        }
    }
}