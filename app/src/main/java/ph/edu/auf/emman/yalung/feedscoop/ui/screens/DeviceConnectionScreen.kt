// File: ui/screens/DeviceConnectionScreen.kt
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
fun DeviceConnectionScreen(
    navController: NavController,
    deviceViewModel: DeviceViewModel = hiltViewModel()
) {
    val isConnected by deviceViewModel.isConnected.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Device Connection", color = Color(0xFF2E7D32)) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isConnected) Color(0xFF2E7D32) else Color.Red)
                    Text(
                        if (isConnected) "FeedScoop prototype is online."
                        else "No device detected. Make sure the prototype is powered on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) Color(0xFF388E3C) else Color(0xFFC62828)
                    )
                }
            }

            if (!isConnected) {
                Button(onClick = { deviceViewModel.connectDevice() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("Connect Device", color = Color.White) }
            } else {
                Button(
                    // FIX: was navigate("real_time_weighing") with no args — caused crash
                    onClick = { navController.navigate("calibration") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("Go to Calibration", color = Color.White) }
                OutlinedButton(onClick = { deviceViewModel.disconnectDevice() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Disconnect Device")
                }
            }
        }
    }
}