// File: ui/screens/SettingsScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", color = Color(0xFF2E7D32)) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Text("Device", style = MaterialTheme.typography.titleSmall, color = Color(0xFF2E7D32))
            SettingsRow("Device Connection") { navController.navigate("device_connection") }
            SettingsRow("Calibration")       { navController.navigate("calibration") }
            Divider()
            Text("Data", style = MaterialTheme.typography.titleSmall, color = Color(0xFF2E7D32))
            SettingsRow("Export Reports") { navController.navigate("reports_export") }
        }
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(">", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
    }
}