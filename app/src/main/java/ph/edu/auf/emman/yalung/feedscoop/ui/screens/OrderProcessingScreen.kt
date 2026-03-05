// File: ui/screens/OrderProcessingScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

// NOTE: The primary order+weighing flow is handled by RealTimeWeighingScreen.
// This screen is kept only to satisfy any legacy navigation references.
// It is safe to delete if all usages have been removed from your nav graph.

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.data.model.InventoryItem
import ph.edu.auf.emman.yalung.feedscoop.data.model.Order
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.InventoryViewModel
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.OrderViewModel

@Composable
fun OrderProcessingScreen(
    navController: NavController,
    productId: String = "",
    orderViewModel: OrderViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val products = inventoryViewModel.inventoryItems.collectAsState().value

    // FIX: use InventoryItem (not Product); match on productId (not id)
    val product: InventoryItem? = products.find { it.productId == productId }

    var kilos by remember { mutableStateOf("") }

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Product not found")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ordering: ${product.name}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Brand: ${product.brand} | ₱${product.pricePerKilo}/kg",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = kilos,
            onValueChange = { kilos = it },
            label = { Text("Kilos to order") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                // FIX: kilosOrdered is Double — was incorrectly cast to Float before
                val kilosOrdered: Double = kilos.toDoubleOrNull() ?: 0.0
                if (kilosOrdered > 0.0) {
                    val order = Order(
                        productId    = product.productId,   // FIX: .productId not .id
                        productName  = product.name,
                        brand        = product.brand,
                        kilosOrdered = kilosOrdered,        // FIX: Double, matches Order model
                        pricePerKilo = product.pricePerKilo,
                        totalPrice   = kilosOrdered * product.pricePerKilo
                        // FIX: removed product.remainingWeight — InventoryItem deduction
                        //      is handled inside OrderViewModel.addOrder via inventoryViewModel
                    )
                    orderViewModel.addOrder(order, inventoryViewModel)
                    navController.navigate("order_result_popup")
                }
            }
        ) {
            Text("Place Order")
        }
    }
}