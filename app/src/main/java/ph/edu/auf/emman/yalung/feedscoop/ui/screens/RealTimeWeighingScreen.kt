// FILE: ui/screens/RealTimeWeighingScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.data.model.Order
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.DeviceViewModel
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.InventoryViewModel
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.OrderViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimeWeighingScreen(
    navController: NavController,
    productId: String,
    productName: String,
    brand: String,
    pricePerKilo: Double,
    orderViewModel: OrderViewModel         = hiltViewModel(),
    deviceViewModel: DeviceViewModel       = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val decodedName  = URLDecoder.decode(productName, StandardCharsets.UTF_8.toString())
    val decodedBrand = URLDecoder.decode(brand,       StandardCharsets.UTF_8.toString())

    var phase           by remember { mutableStateOf("input") }
    var kilosOrderedStr by remember { mutableStateOf("") }

    // ── Live data from DeviceViewModel (sourced from Firebase /device/live) ──
    val currentWeight    by deviceViewModel.currentWeight.collectAsState()
    val cumulativeWeight by deviceViewModel.cumulativeWeight.collectAsState()
    val deviceStatus     by deviceViewModel.deviceStatus.collectAsState()
    val isConnected      by deviceViewModel.isConnected.collectAsState()

    val kilosOrdered = kilosOrderedStr.toDoubleOrNull() ?: 0.0
    val remaining    = (kilosOrdered - cumulativeWeight).coerceAtLeast(0.0)

    // Map Firebase device status to local UI indicator
    val indicatorColor = when (deviceStatus) {
        "MEASURING"     -> Color(0xFFE53935)   // Red — underweight
        "OVERWEIGHT"    -> Color(0xFFF57C00)   // Orange — overweight
        "EXACT",
        "WAIT_DISPENSE" -> Color(0xFF4CAF50)   // Green — exact
        else            -> Color.LightGray
    }
    val indicatorLabel = when (deviceStatus) {
        "MEASURING"     -> "Underweight — Scoop more"
        "OVERWEIGHT"    -> "Overweight — Remove some feed"
        "EXACT",
        "WAIT_DISPENSE" -> "Exact weight reached!"
        "COMPLETE"      -> "Order complete!"
        else            -> if (phase == "scooping") "Waiting for device..." else "—"
    }

    // Auto-complete when device reports WAIT_DISPENSE or COMPLETE
    LaunchedEffect(deviceStatus, phase) {
        if (phase == "scooping" &&
            (deviceStatus == "WAIT_DISPENSE" || deviceStatus == "COMPLETE") &&
            cumulativeWeight > 0.0
        ) {
            val order = Order(
                productId    = productId,
                productName  = decodedName,
                brand        = decodedBrand,
                kilosOrdered = cumulativeWeight,
                pricePerKilo = pricePerKilo,
                totalPrice   = cumulativeWeight * pricePerKilo
            )
            orderViewModel.addOrder(order, inventoryViewModel)
            deviceViewModel.resetOrder()
            navController.navigate("order_result_popup")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weigh: $decodedName") },
                navigationIcon = {
                    IconButton(onClick = {
                        deviceViewModel.resetOrder()
                        navController.popBackStack()
                    }) {
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
            // Device connection badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isConnected) Color(0xFF4CAF50) else Color.Red,
                            RoundedCornerShape(5.dp)
                        )
                )
                Text(
                    text = if (isConnected) "Device online" else "Device offline — connect first",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) Color(0xFF2E7D32) else Color.Red
                )
            }

            Text(
                "Brand: $decodedBrand | ₱$pricePerKilo / kg",
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider()

            // ── Phase: enter target weight ────────────────────────────
            if (phase == "input") {
                Text(
                    "Enter the required weight for this order:",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = kilosOrderedStr,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) kilosOrderedStr = filtered
                    },
                    label = { Text("Kilos Ordered") },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = kilosOrderedStr.isNotBlank() && kilosOrdered <= 0.0,
                    modifier = Modifier.fillMaxWidth()
                )
                if (kilosOrderedStr.isNotBlank() && kilosOrdered <= 0.0)
                    Text(
                        "Enter a valid weight greater than 0",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                if (kilosOrdered > 0.0)
                    Text(
                        "Estimated Total: ₱${String.format("%.2f", kilosOrdered * pricePerKilo)}",
                        style = MaterialTheme.typography.bodySmall
                    )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (kilosOrdered > 0.0) {
                                // Send target weight + start command to Firebase
                                // ESP32 picks this up within ~500ms
                                deviceViewModel.startOrder(kilosOrdered)
                                phase = "scooping"
                            }
                        },
                        enabled = kilosOrdered > 0.0 && isConnected,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Start Scooping", color = Color.White) }

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }

                if (!isConnected) {
                    Text(
                        "⚠ Device is offline. Go to Device Connection first.",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── Phase: active scooping (live data from Firebase) ─────
            if (phase == "scooping") {
                Text(
                    "Target: ${String.format("%.3f", kilosOrdered)} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cumulative: ${String.format("%.3f", cumulativeWeight)} kg")
                        Text(
                            "Remaining: ${String.format("%.3f", remaining)} kg",
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = if (kilosOrdered > 0)
                            (cumulativeWeight / kilosOrdered).toFloat().coerceIn(0f, 1f)
                        else 0f,
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = indicatorColor,
                        trackColor = Color.LightGray
                    )
                }

                // Current scoop weight card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Current Scoop Weight:", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${String.format("%.3f", currentWeight)} kg",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            "Live from device via Firebase",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Status indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(indicatorColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        indicatorLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = indicatorColor
                    )
                }

                // Accept measurement button — writes orderComplete=true to Firebase
                // ESP32 reads it, tares, and resets for next scoop
                Button(
                    onClick = { deviceViewModel.acceptMeasurement() },
                    enabled = isConnected &&
                            currentWeight > 0.0 &&
                            deviceStatus != "OVERWEIGHT" &&
                            deviceStatus != "IDLE",
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("Accept Measurement & Tare", color = Color.White) }

                Text(
                    "Tapping Accept sends a command to the device to zero the scale for the next scoop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                OutlinedButton(
                    onClick = {
                        deviceViewModel.resetOrder()
                        phase = "input"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel Order") }
            }
        }
    }
}