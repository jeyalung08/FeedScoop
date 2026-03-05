// File: ui/screens/OrdersHistoryScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.data.model.Order
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersHistoryScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val orders by orderViewModel.orders.collectAsState()
    val sdf    = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today  = remember { sdf.format(Date()) }

    var startDateStr by remember { mutableStateOf(today) }
    var endDateStr   by remember { mutableStateOf(today) }
    var filterMode   by remember { mutableStateOf("today") } // "today" | "range"

    // Load today's orders by default
    LaunchedEffect(Unit) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay   = startOfDay + 24 * 60 * 60 * 1000 - 1
        orderViewModel.loadOrdersByDateRange(startOfDay, endOfDay)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Orders History", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2E7D32))

        // ── Date filter row ───────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Today button
            FilterChip(
                selected = filterMode == "today",
                onClick = {
                    filterMode = "today"
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
                    }
                    orderViewModel.loadOrdersByDateRange(cal.timeInMillis, cal.timeInMillis + 86_399_999L)
                },
                label = { Text("Today") }
            )

            // Range mode
            FilterChip(
                selected = filterMode == "range",
                onClick = { filterMode = "range" },
                label = { Text("Date Range") }
            )
        }

        if (filterMode == "range") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = startDateStr,
                    onValueChange = { startDateStr = it },
                    label = { Text("Start (yyyy-MM-dd)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endDateStr,
                    onValueChange = { endDateStr = it },
                    label = { Text("End (yyyy-MM-dd)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Button(
                onClick = {
                    runCatching {
                        val start = sdf.parse(startDateStr)?.time ?: return@Button
                        val end   = (sdf.parse(endDateStr)?.time ?: return@Button) + 86_399_999L
                        orderViewModel.loadOrdersByDateRange(start, end)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text("Apply", color = Color.White) }
        }

        Divider()

        // ── Orders list ───────────────────────────────────────────────
        if (orders.isEmpty()) {
            Text("No orders found for the selected period.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Display oldest → newest (bottom to top means first order is shown first in list)
                items(orders) { order ->
                    ExpandableOrderCard(order)
                }
            }
        }
    }
}

@Composable
fun ExpandableOrderCard(order: Order) {
    var expanded by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${order.productName} — ${order.brand}",
                        style = MaterialTheme.typography.titleSmall)
                    Text("${String.format("%.3f", order.kilosOrdered)} kg | ₱${String.format("%.2f", order.totalPrice)}",
                        style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                //Text("Date: ${order.date?.let { sdf.format(it) } ?: "—"}",
                //    style = MaterialTheme.typography.bodySmall)
                Text("Price per kg: ₱${order.pricePerKilo}",
                    style = MaterialTheme.typography.bodySmall)
                Text("Kilos Ordered: ${String.format("%.3f", order.kilosOrdered)} kg",
                    style = MaterialTheme.typography.bodySmall)
                Text("Total Price: ₱${String.format("%.2f", order.totalPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32))
            }
        }
    }
}