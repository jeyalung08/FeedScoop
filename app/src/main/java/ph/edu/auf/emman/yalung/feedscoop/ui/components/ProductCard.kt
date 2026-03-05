package ph.edu.auf.emman.yalung.feedscoop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ph.edu.auf.emman.yalung.feedscoop.data.model.Product

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = product.name)
            Text(text = product.brand)
            Text(text = "${product.pricePerKilo} per kg")
        }
    }
}