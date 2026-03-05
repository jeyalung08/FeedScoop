// File: ui/screens/AvailableProductsScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.data.model.InventoryItem
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.InventoryViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableProductsScreen(
    navController: NavController,
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val products by inventoryViewModel.inventoryItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<InventoryItem?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.brand.contains(searchQuery, ignoreCase = true) ||
                    it.type.contains(searchQuery, ignoreCase = true)
        }
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete \"${item.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    inventoryViewModel.deleteProduct(item.productId)
                    deleteTarget = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Available Products", color = Color(0xFF2E7D32)) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_edit_product?productId=") },
                containerColor = Color(0xFF2E7D32)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, brand, or type") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredProducts, key = { it.productId }) { product ->
                    AvailableProductRow(
                        product  = product,
                        onSelect = {
                            val encodedName  = URLEncoder.encode(product.name,  StandardCharsets.UTF_8.toString())
                            val encodedBrand = URLEncoder.encode(product.brand, StandardCharsets.UTF_8.toString())
                            navController.navigate(
                                "real_time_weighing/${product.productId}/$encodedName/$encodedBrand/${product.pricePerKilo}"
                            )
                        },
                        onEdit   = { navController.navigate("add_edit_product?productId=${product.productId}") },
                        onDelete = { deleteTarget = product }
                    )
                }
            }
        }
    }
}

@Composable
fun AvailableProductRow(
    product: InventoryItem,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLow = product.totalWeight > 0 && product.remainingWeight / product.totalWeight <= 0.10
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text("Brand: ${product.brand} | Type: ${product.type}",
                    style = MaterialTheme.typography.bodySmall)
                Text("Sack Weight: ${product.totalWeight} kg | ${product.pricePerKilo}/kg",
                    style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Remaining: ${String.format("%.2f", product.remainingWeight)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLow) Color.Red else Color.Unspecified
                )
                if (isLow) {
                    Text("Due for Restock", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2E7D32))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}