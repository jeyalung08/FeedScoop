// FILE: ui/screens/ReportsExportScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.data.model.Order
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.OrderViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsExportScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val orders  by orderViewModel.orders.collectAsState()
    val sdf     = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today   = remember { sdf.format(Date()) }

    var startDateStr  by remember { mutableStateOf(today) }
    var endDateStr    by remember { mutableStateOf(today) }
    var filterMode    by remember { mutableStateOf("all") }
    var isExporting   by remember { mutableStateOf(false) }
    var exportedCount by remember { mutableStateOf(0) }

    // Load all orders by default so there's data to export
    LaunchedEffect(Unit) { orderViewModel.loadAllOrders() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Reports", color = Color(0xFF2E7D32)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Filter section ──────────────────────────────────────
            Text("Select Date Range", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterMode == "all",
                    onClick = { filterMode = "all"; orderViewModel.loadAllOrders() },
                    label = { Text("All Time") }
                )
                FilterChip(
                    selected = filterMode == "today",
                    onClick = {
                        filterMode = "today"
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
                        }
                        orderViewModel.loadOrdersByDateRange(
                            cal.timeInMillis,
                            cal.timeInMillis + 86_399_999L
                        )
                    },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = filterMode == "range",
                    onClick = { filterMode = "range" },
                    label = { Text("Custom Range") }
                )
            }

            if (filterMode == "range") {
                OutlinedTextField(
                    value = startDateStr, onValueChange = { startDateStr = it },
                    label = { Text("Start Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = endDateStr, onValueChange = { endDateStr = it },
                    label = { Text("End Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Button(
                    onClick = {
                        runCatching {
                            val start = sdf.parse(startDateStr)?.time ?: return@Button
                            val end = (sdf.parse(endDateStr)?.time ?: return@Button) + 86_399_999L
                            orderViewModel.loadOrdersByDateRange(start, end)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("Apply Filter", color = Color.White) }
            }

            HorizontalDivider()

            // ── Preview summary ─────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Export Preview", style = MaterialTheme.typography.titleSmall)
                    Text("Orders to export: ${orders.size}")
                    if (orders.isNotEmpty()) {
                        Text(
                            "Total Revenue: ₱${String.format("%.2f", orders.sumOf { it.totalPrice })}"
                        )
                        Text(
                            "Total kg sold: ${String.format("%.2f", orders.sumOf { it.kilosOrdered })} kg"
                        )
                        val earliest = orders.minOf { it.timestamp }
                        val latest   = orders.maxOf { it.timestamp }
                        val fmtDate  = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        Text("Date range: ${fmtDate.format(Date(earliest))} – ${fmtDate.format(Date(latest))}")
                    }
                }
            }

            if (exportedCount > 0) {
                Text(
                    "✓ Last export: $exportedCount orders",
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Export button ───────────────────────────────────────
            Button(
                onClick = {
                    if (orders.isEmpty()) {
                        Toast.makeText(context, "No orders to export", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isExporting = true
                    val file = exportOrdersToCsv(context, orders)
                    isExporting = false
                    if (file != null) {
                        exportedCount = orders.size
                        shareFile(context, file)
                    } else {
                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isExporting && orders.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                if (isExporting) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text("Exporting...", color = Color.White)
                    }
                } else {
                    Text(
                        "Export as CSV  (${orders.size} orders)",
                        color = Color.White
                    )
                }
            }

            Text(
                "The CSV file will be shared via your device's share sheet. " +
                        "You can save it to Files, send via email, or open in a spreadsheet app.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

// ── CSV generation ──────────────────────────────────────────────────────────

private fun exportOrdersToCsv(context: Context, orders: List<Order>): File? {
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName  = "feedscoop_orders_$timestamp.csv"
        val file      = File(context.cacheDir, fileName)

        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        file.bufferedWriter().use { writer ->
            // Header row
            writer.write("Order ID,Date,Product Name,Brand,Kilos Ordered,Price per Kilo,Total Price\n")
            // Data rows
            orders.forEach { order ->
                val date = dateFmt.format(Date(order.timestamp))
                // Wrap string fields in quotes to handle commas in names
                writer.write(
                    "${order.id}," +
                            "\"$date\"," +
                            "\"${order.productName}\"," +
                            "\"${order.brand}\"," +
                            "${String.format("%.3f", order.kilosOrdered)}," +
                            "${String.format("%.2f", order.pricePerKilo)}," +
                            "${String.format("%.2f", order.totalPrice)}\n"
                )
            }
            // Summary footer
            writer.write("\n")
            writer.write(",,TOTAL,,${String.format("%.3f", orders.sumOf { it.kilosOrdered })},,${String.format("%.2f", orders.sumOf { it.totalPrice })}\n")
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type    = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "FeedScoop Orders Export")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share CSV via"))
}