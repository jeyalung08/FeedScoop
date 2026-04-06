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
    remainingWeight: Double  = 0.0,
    totalSackWeight: Double  = 0.0,
    orderViewModel: OrderViewModel         = hiltViewModel(),
    deviceViewModel: DeviceViewModel       = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val decodedName  = URLDecoder.decode(productName, StandardCharsets.UTF_8.toString())
    val decodedBrand = URLDecoder.decode(brand,       StandardCharsets.UTF_8.toString())

    var phase             by remember { mutableStateOf("input") }
    var kilosOrderedStr   by remember { mutableStateOf("") }
    var orderSentToDevice by remember { mutableStateOf(false) }
    var showResetConfirm  by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    // Cache last non-zero weight so display doesn't jump to 0 in WAIT_DISPENSE
    var lastDisplayedWeight by remember { mutableStateOf(0.0) }

    val currentWeight    by deviceViewModel.currentWeight.collectAsState()
    val cumulativeWeight by deviceViewModel.cumulativeWeight.collectAsState()
    val deviceStatus     by deviceViewModel.deviceStatus.collectAsState()
    val isConnected      by deviceViewModel.isConnected.collectAsState()

    LaunchedEffect(currentWeight, deviceStatus) {
        when (deviceStatus) {
            "MEASURING", "OVERWEIGHT", "EXACT" ->
                lastDisplayedWeight = currentWeight
            "IDLE" ->
                lastDisplayedWeight = 0.0
        }
    }

    val kilosOrdered  = kilosOrderedStr.toDoubleOrNull() ?: 0.0
    val remaining     = (kilosOrdered - cumulativeWeight).coerceAtLeast(0.0)
    val targetReached = kilosOrdered > 0.0 && cumulativeWeight >= kilosOrdered

    // ── Input validation ─────────────────────────────────────────────
    val inputError: String? = when {
        kilosOrderedStr.isNotBlank() && kilosOrdered <= 0.0 ->
            "Enter a valid weight greater than 0"
        remainingWeight > 0.0 && kilosOrdered > remainingWeight ->
            "Exceeds remaining stock (${String.format("%.3f", remainingWeight)} kg available)"
        totalSackWeight > 0.0 && kilosOrdered > totalSackWeight ->
            "Exceeds sack weight (${String.format("%.3f", totalSackWeight)} kg max)"
        else -> null
    }
    val canStart = kilosOrdered > 0.0 && inputError == null && isConnected

    // ── Status colors and labels ─────────────────────────────────────
    val indicatorColor: Color = when {
        deviceStatus == "OVERWEIGHT"                                -> Color(0xFFF57C00)
        deviceStatus == "EXACT" || deviceStatus == "WAIT_DISPENSE" -> Color(0xFF4CAF50)
        deviceStatus == "WAIT_TARE"                                 -> Color(0xFF1565C0)
        deviceStatus == "COMPLETE"                                  -> Color(0xFF2E7D32)
        targetReached                                               -> Color(0xFF4CAF50)
        deviceStatus == "MEASURING"                                 -> Color(0xFFE53935)
        else                                                        -> Color.LightGray
    }
    val indicatorLabel: String = when {
        deviceStatus == "OVERWEIGHT"    -> "Overweight — remove some feed"
        deviceStatus == "WAIT_DISPENSE" -> "Weight locked — tap Accept or Proceed"
        deviceStatus == "WAIT_TARE"     -> "Scoop accepted — tap Tare to continue"
        deviceStatus == "EXACT"         -> "Exact weight — tap Accept or Proceed"
        deviceStatus == "COMPLETE"      -> "Order complete!"
        targetReached                   -> "Target reached — tap Proceed to finish"
        deviceStatus == "MEASURING"     -> "Scooping — keep adding feed"
        phase == "scooping"             -> "Waiting for device..."
        else                            -> "—"
    }

    // ── Device reboot detection ──────────────────────────────────────
    LaunchedEffect(deviceStatus) {
        if (phase == "scooping" && orderSentToDevice &&
            deviceStatus == "IDLE" && isConnected) {
            deviceViewModel.startOrder(kilosOrdered)
        }
    }

    // ── Auto-navigate when firmware reports COMPLETE ─────────────────
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
            phase               = "input"
            kilosOrderedStr     = ""
            lastDisplayedWeight = 0.0
            orderSentToDevice   = false
            navController.navigate("order_result_popup")
        }
    }

    // ── Also reset screen state when device returns to IDLE mid-order ─
    LaunchedEffect(deviceStatus) {
        if (phase == "scooping" && deviceStatus == "IDLE" && !orderSentToDevice) {
            phase               = "input"
            kilosOrderedStr     = ""
            lastDisplayedWeight = 0.0
        }
    }

    // ── Reset cumulative confirmation dialog ─────────────────────────
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Cumulative Weight?") },
            text  = {
                Text(
                    "This will zero the cumulative total " +
                            "(${String.format("%.3f", cumulativeWeight)} kg) " +
                            "and tare the scale.\n\n" +
                            "Target (${String.format("%.3f", kilosOrdered)} kg) stays the same."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    deviceViewModel.resetCumulative(kilosOrdered)
                }) { Text("Reset", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ── Cancel order confirmation dialog ─────────────────────────────
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel Order?") },
            text  = {
                Text(
                    "This will cancel the entire order and return the device to idle. " +
                            "No weight will be recorded."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirm = false
                    deviceViewModel.resetOrder()
                    orderSentToDevice   = false
                    lastDisplayedWeight = 0.0
                    phase = "input"
                }) { Text("Cancel Order", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep Going") }
            }
        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                "Brand: $decodedBrand  |  ₱$pricePerKilo / kg",
                style = MaterialTheme.typography.bodyMedium
            )

            // Stock info bar
            if (remainingWeight > 0.0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (remainingWeight / totalSackWeight <= 0.10)
                            Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Available stock:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${String.format("%.3f", remainingWeight)} kg" +
                                    " / ${String.format("%.3f", totalSackWeight)} kg",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (remainingWeight / totalSackWeight <= 0.10)
                                Color.Red else Color(0xFF2E7D32)
                        )
                    }
                }
            }

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
                    value         = kilosOrderedStr,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) kilosOrderedStr = filtered
                    },
                    label    = { Text("Kilos Ordered") },
                    suffix   = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError  = inputError != null,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (inputError != null)
                            Text(inputError, color = MaterialTheme.colorScheme.error)
                        else if (remainingWeight > 0.0)
                            Text("Max: ${String.format("%.3f", remainingWeight)} kg",
                                color = Color.Gray)
                    }
                )

                if (kilosOrdered > 0.0 && inputError == null)
                    Text(
                        "Estimated total: ₱${String.format("%.2f", kilosOrdered * pricePerKilo)}",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray
                    )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick  = {
                            deviceViewModel.startOrder(kilosOrdered)
                            lastDisplayedWeight = 0.0
                            orderSentToDevice   = true
                            phase = "scooping"
                        },
                        enabled  = canStart,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) { Text("Start Scooping", color = Color.White) }

                    OutlinedButton(
                        onClick  = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }

                if (!isConnected)
                    Text(
                        "⚠ Device is offline. Go to Device Connection to check status.",
                        color = Color.Red, style = MaterialTheme.typography.bodySmall
                    )
            }

            // ════════════════════════════════════════════════════════
            // PHASE: SCOOPING
            // ════════════════════════════════════════════════════════
            if (phase == "scooping") {

                // Reboot banner
                if (deviceStatus == "IDLE" && isConnected && orderSentToDevice) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚠ Device restarted", style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            Text("Resending order automatically...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100))
                        }
                    }
                }

                // Target + progress
                Text(
                    "Target: ${String.format("%.3f", kilosOrdered)} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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

                // Live weight card
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
                            style      = MaterialTheme.typography.headlineMedium,
                            color      = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text  = if (deviceStatus == "WAIT_DISPENSE") "Weight locked"
                            else "Live from device",
                            style = MaterialTheme.typography.bodySmall, color = Color.LightGray
                        )
                    }
                }

                // Status box
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

                HorizontalDivider()

                // ── BUTTON 1: ACCEPT ──────────────────────────────────
                val acceptEnabled = isConnected &&
                        deviceStatus != "WAIT_TARE" && (
                        (deviceStatus == "MEASURING" && currentWeight > 0.0) ||
                                deviceStatus == "WAIT_DISPENSE" ||
                                deviceStatus == "EXACT"
                        )
                Button(
                    onClick  = { deviceViewModel.acceptMeasurement() },
                    enabled  = acceptEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("✔  Accept Measurement", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Adds current scoop weight to cumulative total and tares the scale.",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )

                // ── BUTTON 2: TARE ────────────────────────────────────
                val tareEnabled = isConnected && (
                        deviceStatus == "MEASURING"    ||
                                deviceStatus == "WAIT_DISPENSE"||
                                deviceStatus == "WAIT_TARE"    ||
                                deviceStatus == "OVERWEIGHT"   ||
                                deviceStatus == "EXACT"
                        )
                val tareIsRequired = deviceStatus == "WAIT_TARE"
                Button(
                    onClick  = { deviceViewModel.tareScale() },
                    enabled  = tareEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (tareIsRequired) Color(0xFF2E7D32)
                        else Color(0xFF546E7A)
                    )
                ) {
                    Text(
                        if (tareIsRequired) "⊖  Tare Scale to Continue"
                        else "⊖  Tare Scale (Zero Only)",
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    if (tareIsRequired)
                        "Tap Tare to zero the scale before the next scoop."
                    else
                        "Zeros the scale without adding to the cumulative weight.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tareIsRequired) Color(0xFF2E7D32) else Color.Gray
                )

                // ── BUTTON 3: PROCEED TO CHECKOUT ─────────────────────
                if (deviceStatus == "WAIT_DISPENSE" || deviceStatus == "EXACT"
                    || targetReached) {
                    Button(
                        onClick  = {
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
                        },
                        enabled  = isConnected && cumulativeWeight > 0.0,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛒  Proceed to Checkout",
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

                // ── BUTTON 4: RESET CUMULATIVE ────────────────────────
                OutlinedButton(
                    onClick  = { showResetConfirm = true },
                    enabled  = deviceStatus != "WAIT_DISPENSE" && deviceStatus != "EXACT",
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE65100)
                    )
                ) { Text("↺  Reset Cumulative Weight") }

                // ── BUTTON 5: CANCEL ORDER ────────────────────────────
                OutlinedButton(
                    onClick  = { showCancelConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) { Text("✕  Cancel Order") }
            }
        }
    }
}