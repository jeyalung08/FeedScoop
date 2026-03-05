package ph.edu.auf.emman.yalung.feedscoop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ph.edu.auf.emman.yalung.feedscoop.data.model.Order

@Composable
fun OrderSummaryCard(order: Order) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = order.productName)
            Text(text = "${order.kilosOrdered} kg at ${order.pricePerKilo}/kg")
            Text(text = "Total: ${order.totalPrice}")
        }
    }
}