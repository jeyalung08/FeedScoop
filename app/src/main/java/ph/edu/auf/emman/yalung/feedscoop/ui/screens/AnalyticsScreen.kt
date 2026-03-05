// File: ui/screens/AnalyticsScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val orders  by orderViewModel.orders.collectAsState()
    val sdf      = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today    = remember { sdf.format(Date()) }

    var filterMode   by remember { mutableStateOf("all") } // "all" | "day" | "range"
    var startDateStr by remember { mutableStateOf(today) }
    var endDateStr   by remember { mutableStateOf(today) }

    // Analytics derivations
    val totalProfit = orders.sumOf { it.totalPrice }

    val productKilos = orders
        .groupBy { it.productName }
        .mapValues { (_, v) -> v.sumOf { it.kilosOrdered } }
        .toList()
        .sortedByDescending { it.second }

    val topFeed      = productKilos.firstOrNull()?.first ?: "—"
    val avgKg        = if (orders.isEmpty()) 0.0 else orders.sumOf { it.kilosOrdered } / orders.size
    val maxKilos     = productKilos.maxOfOrNull { it.second } ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Analytics", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2E7D32))

        // ── Filter controls ───────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterMode == "all",
                onClick  = {
                    filterMode = "all"
                    orderViewModel.loadAllOrders()
                },
                label = { Text("All") }
            )
            FilterChip(
                selected = filterMode == "day",
                onClick  = { filterMode = "day" },
                label = { Text("Day") }
            )
            FilterChip(
                selected = filterMode == "range",
                onClick  = { filterMode = "range" },
                label = { Text("Range") }
            )
        }

        if (filterMode == "day") {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startDateStr,
                    onValueChange = { startDateStr = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = {
                    runCatching {
                        val t = sdf.parse(startDateStr)?.time ?: return@Button
                        orderViewModel.loadOrdersByDateRange(t, t + 86_399_999L)
                    }
                }) { Text("Go") }
            }
        }

        if (filterMode == "range") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = startDateStr, onValueChange = { startDateStr = it },
                    label = { Text("Start") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = endDateStr, onValueChange = { endDateStr = it },
                    label = { Text("End") }, modifier = Modifier.weight(1f), singleLine = true
                )
            }
            Button(onClick = {
                runCatching {
                    val s = sdf.parse(startDateStr)?.time ?: return@Button
                    val e = (sdf.parse(endDateStr)?.time ?: return@Button) + 86_399_999L
                    orderViewModel.loadOrdersByDateRange(s, e)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                Text("Apply Range", color = Color.White)
            }
        }

        Divider()

        // ── Profit card ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Revenue", style = MaterialTheme.typography.titleMedium)
                Text(
                    "₱${String.format("%.2f", totalProfit)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        // ── Trend summary ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Trends", style = MaterialTheme.typography.titleMedium)
                Text("🏆 Top Feed Sold: $topFeed")
                Text("⚖ Avg kg per Order: ${String.format("%.2f", avgKg)} kg")
                Text("📦 Total Orders: ${orders.size}")
            }
        }

        // ── Bar chart: kg sold per product ───────────────────────────
        if (productKilos.isNotEmpty()) {
            Text("Kilograms Sold by Product", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                productKilos.forEach { (name, kilos) ->
                    AnalyticsBarItem(productName = name, kilosSold = kilos, maxKilos = maxKilos)
                }
            }
        } else {
            Text("No data to display.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AnalyticsBarItem(productName: String, kilosSold: Double, maxKilos: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(productName, style = MaterialTheme.typography.bodySmall)
            Text("${String.format("%.1f", kilosSold)} kg", style = MaterialTheme.typography.bodySmall)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (kilosSold / maxKilos).toFloat().coerceIn(0f, 1f))
                    .height(18.dp)
                    .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
            )
        }
    }
}