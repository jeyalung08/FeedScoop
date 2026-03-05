// File: ui/screens/RealTimeWeighingScreen.kt
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

private enum class WeightStatus { IDLE, UNDERWEIGHT, OVERWEIGHT, EXACT }

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

    val kilosOrdered = kilosOrderedStr.toDoubleOrNull() ?: 0.0
    val remaining    = (kilosOrdered - cumulativeWeight).coerceAtLeast(0.0)

    val weightStatus: WeightStatus = when {
        phase != "scooping"                                -> WeightStatus.IDLE
        cumulativeWeight <= 0.0 && currentWeight <= 0.0   -> WeightStatus.IDLE
        (cumulativeWeight + currentWeight) < kilosOrdered -> WeightStatus.UNDERWEIGHT
        (cumulativeWeight + currentWeight) > kilosOrdered -> WeightStatus.OVERWEIGHT
        else                                               -> WeightStatus.EXACT
    }

    val indicatorColor = when (weightStatus) {
        WeightStatus.UNDERWEIGHT -> Color(0xFFE53935)
        WeightStatus.OVERWEIGHT  -> Color(0xFFF57C00)
        WeightStatus.EXACT       -> Color(0xFF4CAF50)
        WeightStatus.IDLE        -> Color.LightGray
    }
    val indicatorLabel = when (weightStatus) {
        WeightStatus.UNDERWEIGHT -> "Underweight - Scoop more"
        WeightStatus.OVERWEIGHT  -> "Overweight - Remove some feed"
        WeightStatus.EXACT       -> "Exact weight reached!"
        WeightStatus.IDLE        -> "-"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weigh: $decodedName") },
                navigationIcon = {
                    IconButton(onClick = { deviceViewModel.resetWeight(); navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Brand: $decodedBrand | $pricePerKilo / kg", style = MaterialTheme.typography.bodyMedium)
            Divider()

            if (phase == "input") {
                Text("Enter the required weight for this order:", style = MaterialTheme.typography.bodyLarge)
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
                    Text("Enter a valid weight greater than 0", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                if (kilosOrdered > 0.0)
                    Text("Estimated Total: ${String.format("%.2f", kilosOrdered * pricePerKilo)}", style = MaterialTheme.typography.bodySmall)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { if (kilosOrdered > 0.0) { deviceViewModel.resetWeight(); phase = "scooping" } },
                        enabled = kilosOrdered > 0.0, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Scoop", color = Color.White) }
                    OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                }
            }

            if (phase == "scooping") {
                Text("Target: ${String.format("%.3f", kilosOrdered)} kg",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cumulative: ${String.format("%.3f", cumulativeWeight)} kg")
                        Text("Remaining: ${String.format("%.3f", remaining)} kg", color = Color(0xFF2E7D32))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = if (kilosOrdered > 0) (cumulativeWeight / kilosOrdered).toFloat().coerceIn(0f, 1f) else 0f,
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = indicatorColor, trackColor = Color.LightGray
                    )
                }

                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Current Scoop Weight:", style = MaterialTheme.typography.labelMedium)
                        Text("${String.format("%.3f", currentWeight)} kg",
                            style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32))
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(indicatorColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(indicatorLabel, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold, color = indicatorColor)
                }

                Button(
                    onClick = { deviceViewModel.acceptMeasurement() },
                    enabled = currentWeight > 0.0 && weightStatus != WeightStatus.EXACT,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) { Text("Accept Measurement & Tare", color = Color.White) }

                LaunchedEffect(cumulativeWeight) {
                    if (phase == "scooping" && kilosOrdered > 0.0 && cumulativeWeight >= kilosOrdered * 0.999) {
                        val order = Order(
                            productId = productId, productName = decodedName, brand = decodedBrand,
                            kilosOrdered = cumulativeWeight, pricePerKilo = pricePerKilo,
                            totalPrice = cumulativeWeight * pricePerKilo
                        )
                        orderViewModel.addOrder(order, inventoryViewModel)
                        deviceViewModel.resetWeight()
                        navController.navigate("order_result_popup")
                    }
                }

                // Resets cumulative total back to zero so user can restart weighing
                OutlinedButton(onClick = { deviceViewModel.resetWeight() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset Total Weight")
                }
                OutlinedButton(onClick = { deviceViewModel.resetWeight(); phase = "input" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel Order")
                }
            }
        }
    }
}