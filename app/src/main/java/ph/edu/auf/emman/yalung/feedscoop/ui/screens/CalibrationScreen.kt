// File: ui/screens/CalibrationScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calibration", color = Color(0xFF2E7D32)) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Calibration Status", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(calibrationStatus, style = MaterialTheme.typography.titleMedium,
                        color = when {
                            calibrationStatus.contains("Complete") -> Color(0xFF2E7D32)
                            calibrationStatus.contains("...") -> Color(0xFFF57C00)
                            else -> Color.Gray
                        })
                }
            }
            Text("Place the empty scoop on a flat surface before starting calibration.",
                style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { deviceViewModel.startCalibration() }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text("Start Calibration", color = Color.White) }
            // FIX: was navigate("real_time_weighing") with no args — caused crash
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}