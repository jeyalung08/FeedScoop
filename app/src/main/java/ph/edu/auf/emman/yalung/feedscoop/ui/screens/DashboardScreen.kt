// File: ui/screens/DashboardScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun DashboardScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "FeedScoop Dashboard",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── BTN_START: "Start an Order" ──────────────────────────────
        Button(
            onClick = { navController.navigate("available_products") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(
                text = "Start an Order",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Navigation tiles ────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardRow(
                navController = navController,
                items = listOf(
                    DashboardItem("Available Products", "available_products"),
                    DashboardItem("Inventory", "inventory_management")
                )
            )
            DashboardRow(
                navController = navController,
                items = listOf(
                    DashboardItem("Orders History", "orders_history"),
                    DashboardItem("Analytics", "analytics")
                )
            )
            DashboardRow(
                navController = navController,
                items = listOf(
                    DashboardItem("Device Connection", "device_connection"),
                    DashboardItem("Settings", "settings")
                )
            )
        }
    }
}

data class DashboardItem(val title: String, val route: String)

@Composable
fun DashboardRow(navController: NavController, items: List<DashboardItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clickable { navController.navigate(item.route) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}