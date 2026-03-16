// FILE: ui/screens/RealTimeWeighingScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

    val currentWeight    by deviceViewModel.currentWeight.collectAsState()
    val cumulativeWeight by deviceViewModel.cumulativeWeight.collectAsState()
    val deviceStatus     by deviceViewModel.deviceStatus.collectAsState()
    val isConnected      by deviceViewModel.isConnected.collectAsState()

    // Cache last non-zero weight so display doesn't jump to 0 in WAIT_DISPENSE
    var lastDisplayedWeight by remember { mutableStateOf(0.0) }
    LaunchedEffect(currentWeight, deviceStatus) {
        if (deviceStatus == "MEASURING" || deviceStatus == "OVERWEIGHT" || deviceStatus == "EXACT") {
            lastDisplayedWeight = currentWeight
        }
    }

    val kilosOrdered  = kilosOrderedStr.toDoubleOrNull() ?: 0.0
    val remaining     = (kilosOrdered - cumulativeWeight).coerceAtLeast(0.0)
    val targetReached = kilosOrdered > 0.0 && cumulativeWeight >= kilosOrdered

    // Status color
    val indicatorColor: Color = when {
        deviceStatus == "OVERWEIGHT"                               -> Color(0xFFF57C00)
        deviceStatus == "EXACT" || deviceStatus == "WAIT_DISPENSE" -> Color(0xFF4CAF50)
        deviceStatus == "COMPLETE"                                 -> Color(0xFF2E7D32)
        targetReached                                              -> Color(0xFF4CAF50)
        deviceStatus == "MEASURING"                                -> Color(0xFFE53935)
        else                                                       -> Color.LightGray
    }

    // Status label
    val indicatorLabel: String = when {
        deviceStatus == "OVERWEIGHT"    -> "Overweight — remove some feed"
        deviceStatus == "WAIT_DISPENSE" -> "Weight locked — tap Accept or Proceed"
        deviceStatus == "EXACT"         -> "Exact weight — tap Accept or Proceed"
        deviceStatus == "COMPLETE"      -> "Order complete!"
        targetReached                   -> "Target reached — tap Proceed to finish"
        deviceStatus == "MEASURING"     -> "Scooping — keep adding feed"
        phase == "scooping"             -> "Waiting for device..."
        else                            -> "—"
    }

    // Auto-navigate when firmware reports COMPLETE
    LaunchedEffect(deviceStatus) {
        if (phase == "scooping" && deviceStatus == "COMPLETE" && cumulativeWeight > 0.0) {
            val order = Order(
                productId    = productId,
                productName  = decodedName,
                brand        = decodedBrand,
                kilosOrdered = cumulativeWeight,
                pricePerKilo = pricePerKilo,
                totalPrice   = cumulativeWeight * pricePerKilo
            )
            orderViewModel.addOrder(order, inventoryViewModel)
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Device online dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    text  = if (isConnected) "Device online" else "Device offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) Color(0xFF2E7D32) else Color.Red
                )
            }

            Text(
                "Brand: $decodedBrand  |  ₱$pricePerKilo / kg",
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider()

            // ════════════════════════════════════════════════════════
            // PHASE: INPUT
            // ════════════════════════════════════════════════════════
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
                    label   = { Text("Kilos Ordered") },
                    suffix  = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = kilosOrderedStr.isNotBlank() && kilosOrdered <= 0.0,
                    modifier = Modifier.fillMaxWidth()
                )

                if (kilosOrderedStr.isNotBlank() && kilosOrdered <= 0.0)
                    Text("Enter a valid weight greater than 0",
                        color = Color.Red, style = MaterialTheme.typography.bodySmall)

                if (kilosOrdered > 0.0)
                    Text(
                        "Estimated total: ₱${String.format("%.2f", kilosOrdered * pricePerKilo)}",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray
                    )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (kilosOrdered > 0.0) {
                                deviceViewModel.startOrder(kilosOrdered)
                                lastDisplayedWeight = 0.0
                                phase = "scooping"
                            }
                        },
                        enabled  = kilosOrdered > 0.0 && isConnected,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Start Scooping", color = Color.White) }

                    OutlinedButton(
                        onClick  = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }

                if (!isConnected)
                    Text("⚠ Device is offline. Go to Device Connection to check status.",
                        color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            // ════════════════════════════════════════════════════════
            // PHASE: SCOOPING
            // ════════════════════════════════════════════════════════
            if (phase == "scooping") {

                // Target + progress
                Text(
                    "Target: ${String.format("%.3f", kilosOrdered)} kg",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cumulative: ${String.format("%.3f", cumulativeWeight)} kg")
                        Text(
                            text       = if (targetReached) "✓ Target reached"
                            else "Remaining: ${String.format("%.3f", remaining)} kg",
                            color      = Color(0xFF2E7D32),
                            fontWeight = if (targetReached) FontWeight.Bold else FontWeight.Normal,
                            style      = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress   = if (kilosOrdered > 0)
                            (cumulativeWeight / kilosOrdered).toFloat().coerceIn(0f, 1f)
                        else 0f,
                        modifier   = Modifier.fillMaxWidth().height(12.dp),
                        color      = indicatorColor,
                        trackColor = Color.LightGray
                    )
                }

                // Current scoop weight — shows cached value in WAIT_DISPENSE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Current Scoop Weight",
                            style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(
                            text = when (deviceStatus) {
                                "WAIT_DISPENSE", "EXACT" ->
                                    "${String.format("%.3f", lastDisplayedWeight)} kg"
                                else ->
                                    "${String.format("%.3f", currentWeight)} kg"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold
                        )
                        Text(
                            text  = if (deviceStatus == "WAIT_DISPENSE") "Weight locked"
                            else "Live from device",
                            style = MaterialTheme.typography.bodySmall, color = Color.LightGray
                        )
                    }
                }

                // Status indicator box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(indicatorColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(indicatorLabel, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold, color = indicatorColor)
                }

                // ── Accept + Tare ─────────────────────────────────────
                // Accept: adds current scoop to cumulative.
                //   - Available in MEASURING when currentWeight > 0
                //   - Available in WAIT_DISPENSE (exact weight auto-detected)
                //
                // Tare: zeroes the scale.
                //   - Only available in MEASURING (not locked in WAIT_DISPENSE)
                //   - Use after Accept to zero scale for next scoop
                //
                // Accept: adds current scoop to cumulative.
                //   - MEASURING:     enabled when currentWeight > 0
                //   - WAIT_DISPENSE: enabled (exact weight was auto-detected)
                val acceptEnabled = isConnected && (
                        (deviceStatus == "MEASURING" && currentWeight > 0.0) ||
                                (deviceStatus == "WAIT_DISPENSE")
                        )

                // Tare: zeroes the scale — enabled during ANY active order state.
                // User can tare freely between scoops, after overweight,
                // or whenever needed throughout a multi-scoop order.
                // Only disabled when IDLE or COMPLETE (no active order).
                val tareEnabled = isConnected && (
                        deviceStatus == "MEASURING"     ||
                                deviceStatus == "WAIT_DISPENSE" ||
                                deviceStatus == "OVERWEIGHT"    ||
                                deviceStatus == "EXACT"
                        )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = { deviceViewModel.acceptMeasurement() },
                        enabled  = acceptEnabled,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick  = { deviceViewModel.tareScale() },
                        enabled  = tareEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tare", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add scoop to total",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                        modifier = Modifier.weight(1f))
                    Text("Reset scale to 0",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                        modifier = Modifier.weight(1f))
                }

                // ── PROCEED button ─────────────────────────────────────
                // Appears when target is reached or device is in WAIT_DISPENSE/EXACT.
                // Saves the order and navigates to result screen.
                // Calls resetOrder() which writes orderCancelled=true so the
                // firmware stops beeping and returns to IDLE cleanly.
                if (deviceStatus == "WAIT_DISPENSE"
                    || deviceStatus == "EXACT"
                    || targetReached
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val order = Order(
                                productId    = productId,
                                productName  = decodedName,
                                brand        = decodedBrand,
                                kilosOrdered = cumulativeWeight,
                                pricePerKilo = pricePerKilo,
                                totalPrice   = cumulativeWeight * pricePerKilo
                            )
                            orderViewModel.addOrder(order, inventoryViewModel)
                            deviceViewModel.resetOrder()   // writes orderCancelled=true
                            navController.navigate("order_result_popup")
                        },
                        enabled  = isConnected && cumulativeWeight > 0.0,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Proceed with Order",
                                color = Color.White, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${String.format("%.3f", cumulativeWeight)} kg  —  " +
                                        "₱${String.format("%.2f", cumulativeWeight * pricePerKilo)}",
                                color = Color.White, style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick  = {
                        deviceViewModel.resetOrder()   // writes orderCancelled=true
                        phase = "input"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel Order") }
            }
        }
    }
}