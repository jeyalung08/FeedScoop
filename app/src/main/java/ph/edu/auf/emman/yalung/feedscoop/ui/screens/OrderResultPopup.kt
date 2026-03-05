// File: ui/screens/OrderResultPopup.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.data.model.Order
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.OrderViewModel

@Composable
fun OrderResultPopup(
    navController: NavController,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val currentOrders = orderViewModel.currentOrders.collectAsState().value
    val totalPrice: Double = currentOrders.sumOf { it.totalPrice }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Order Result",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Show all orders accumulated in this checkout session
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentOrders) { order ->
                OrderResultRow(order)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Grand Total: ₱${String.format("%.2f", totalPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = "${currentOrders.size} item(s)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add another order — go back to product selection
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("available_products") }
            ) {
                Text(text = "Add Order")
            }

            // Finish — go to full order summary
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("order_summary") {
                        popUpTo("available_products") { inclusive = false }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text(text = "Finish", color = Color.White)
            }
        }
    }
}

@Composable
fun OrderResultRow(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = order.productName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Brand: ${order.brand}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${String.format("%.3f", order.kilosOrdered)} kg  ×  ₱${order.pricePerKilo}/kg",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Subtotal: ₱${String.format("%.2f", order.totalPrice)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20)
            )
        }
    }
}