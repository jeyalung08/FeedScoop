// File: ui/screens/OrderSummaryScreen.kt
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
fun OrderSummaryScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val allOrders   = orderViewModel.currentOrders.collectAsState().value
    val totalPrice  = allOrders.sumOf { it.totalPrice }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Order Summary",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allOrders) { order ->
                SummaryOrderRow(order)
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
                    text = "${allOrders.size} sack order(s)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                orderViewModel.clearCurrentOrders()
                navController.navigate("dashboard") {
                    popUpTo("dashboard") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(text = "Finish and Return to Dashboard", color = Color.White)
        }
    }
}

@Composable
fun SummaryOrderRow(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = order.productName, style = MaterialTheme.typography.titleSmall)
            Text(text = "Brand: ${order.brand}", style = MaterialTheme.typography.bodySmall)
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