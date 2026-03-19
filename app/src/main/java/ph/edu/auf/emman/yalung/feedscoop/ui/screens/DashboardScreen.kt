// FILE: ui/screens/DashboardScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.DeviceViewModel
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.InventoryViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    inventoryViewModel: InventoryViewModel = hiltViewModel(),
    deviceViewModel: DeviceViewModel       = hiltViewModel()
) {
    val items         by inventoryViewModel.inventoryItems.collectAsState()
    val buttonPressed by deviceViewModel.buttonPressed.collectAsState()

    // When the physical button on the scoop is pressed (in IDLE),
    // navigate directly to AvailableProductsScreen so the user
    // can select a product and start an order.
    LaunchedEffect(buttonPressed) {
        if (buttonPressed) {
            deviceViewModel.clearButtonPressed()
            navController.navigate("available_products")
        }
    }

    val lowStockItems = remember(items) {
        items.filter {
            it.totalWeight > 0 &&
                    it.remainingWeight > 0.0 &&
                    it.remainingWeight / it.totalWeight <= 0.10
        }
    }
    val outOfStockItems = remember(items) {
        items.filter { it.remainingWeight <= 0.0 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text  = "FeedScoop Dashboard",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2E7D32)
        )

        // ── Restock alert banners ───────────────────────────────────
        if (outOfStockItems.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("inventory_management") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🚫", style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${outOfStockItems.size} product(s) out of stock",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            outOfStockItems.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828)
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                }
            }
        }

        if (lowStockItems.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("inventory_management") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠️", style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${lowStockItems.size} product(s) due for restock",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            lowStockItems.joinToString(", ") {
                                "${it.name} (${String.format("%.2f", it.remainingWeight)} kg left)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Primary action ──────────────────────────────────────────
        Button(
            onClick  = { navController.navigate("available_products") },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text("Start an Order", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Navigation tiles ────────────────────────────────────────
        Text("Products & Orders", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        DashboardRow(navController, listOf(
            DashboardItem("Available\nProducts", "available_products"),
            DashboardItem("Inventory",           "inventory_management")
        ))
        DashboardRow(navController, listOf(
            DashboardItem("Orders\nHistory", "orders_history"),
            DashboardItem("Analytics",       "analytics")
        ))

        Spacer(modifier = Modifier.height(4.dp))
        Text("Device & Data", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        DashboardRow(navController, listOf(
            DashboardItem("Device\nConnection", "device_connection"),
            DashboardItem("Calibration",        "calibration")
        ))
        DashboardRow(navController, listOf(
            DashboardItem("Export\nReports", "reports_export"),
            DashboardItem("Settings",        "settings")
        ))
    }
}

data class DashboardItem(val title: String, val route: String)

@Composable
fun DashboardRow(navController: NavController, items: List<DashboardItem>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clickable { navController.navigate(item.route) },
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text      = item.title,
                        style     = MaterialTheme.typography.titleSmall,
                        color     = Color(0xFF1B5E20),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}