// FILE: ui/screens/InventoryManagementScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.data.model.InventoryItem
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryManagementScreen(
    navController: NavController,
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val items by inventoryViewModel.inventoryItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<InventoryItem?>(null) }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.brand.contains(searchQuery, ignoreCase = true) ||
                    it.type.contains(searchQuery, ignoreCase = true)
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete \"${item.name}\"? This cannot be undone.") },
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
            TopAppBar(title = { Text("Inventory Management", color = Color(0xFF2E7D32)) })
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

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No products yet. Tap + to add one."
                        else "No products match \"$searchQuery\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredItems, key = { it.productId }) { item ->
                        InventoryItemCard(
                            item     = item,
                            onEdit   = { navController.navigate("add_edit_product?productId=${item.productId}") },
                            onDelete = { deleteTarget = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLow = item.totalWeight > 0 && item.remainingWeight / item.totalWeight <= 0.10
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Name + action buttons on same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2E7D32))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }

            InventoryDetailRow(label = "Brand",       value = item.brand)
            InventoryDetailRow(label = "Type",        value = item.type)
            InventoryDetailRow(label = "Sack Weight", value = "${item.totalWeight} kg")
            InventoryDetailRow(label = "Remaining",   value = "${String.format("%.3f", item.remainingWeight)} kg")
            InventoryDetailRow(label = "Price",       value = "₱${item.pricePerKilo} / kg")

            if (isLow) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "⚠ Due for Restock",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun InventoryDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}