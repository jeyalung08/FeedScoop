// File: ui/screens/AddEditProductScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ph.edu.auf.emman.yalung.feedscoop.data.model.InventoryItem
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.InventoryViewModel

@Composable
private fun DecimalField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || it == '.' }
            if (filtered.count { it == '.' } <= 1) onValueChange(filtered)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = value.isNotBlank() && value.toDoubleOrNull() == null,
        modifier = Modifier.fillMaxWidth()
    )
    if (value.isNotBlank() && value.toDoubleOrNull() == null) {
        Text("Please enter a valid number", color = Color.Red, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    navController: NavController,
    productId: String = "",
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val products by inventoryViewModel.inventoryItems.collectAsState()
    val existingProduct = remember(products, productId) { products.find { it.productId == productId } }
    val isEditing = productId.isNotBlank()

    var name         by remember { mutableStateOf(existingProduct?.name                        ?: "") }
    var brand        by remember { mutableStateOf(existingProduct?.brand                       ?: "") }
    var type         by remember { mutableStateOf(existingProduct?.type                        ?: "") }
    var totalWeight  by remember { mutableStateOf(existingProduct?.totalWeight?.toString()     ?: "") }
    var remainWeight by remember { mutableStateOf(existingProduct?.remainingWeight?.toString() ?: "") }
    var pricePerKilo by remember { mutableStateOf(existingProduct?.pricePerKilo?.toString()    ?: "") }

    LaunchedEffect(existingProduct) {
        existingProduct?.let {
            name = it.name; brand = it.brand; type = it.type
            totalWeight = it.totalWeight.toString()
            remainWeight = it.remainingWeight.toString()
            pricePerKilo = it.pricePerKilo.toString()
        }
    }

    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Product" else "Add Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (errorMessage.isNotBlank())
                Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = brand, onValueChange = { brand = it },
                label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = type, onValueChange = { type = it },
                label = { Text("Type (e.g. Poultry, Swine)") }, modifier = Modifier.fillMaxWidth())

            DecimalField(totalWeight,  { totalWeight  = it }, "Total Weight of Sack (kg)")
            DecimalField(remainWeight, { remainWeight = it }, "Current / Remaining Weight (kg)")
            DecimalField(pricePerKilo, { pricePerKilo = it }, "Price per Kilo")

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val tw = totalWeight.toDoubleOrNull(); val rw = remainWeight.toDoubleOrNull()
                    val ppk = pricePerKilo.toDoubleOrNull()
                    if (name.isBlank() || brand.isBlank() || tw == null || rw == null || ppk == null) {
                        errorMessage = "Please fill in all required fields with valid numbers."
                        return@Button
                    }
                    val item = InventoryItem(
                        productId = if (isEditing) productId else "", name = name.trim(),
                        brand = brand.trim(), type = type.trim(),
                        totalWeight = tw, remainingWeight = rw, pricePerKilo = ppk
                    )
                    if (isEditing) inventoryViewModel.updateProduct(item) else inventoryViewModel.addProduct(item)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text(if (isEditing) "Save Changes" else "Add Product", color = Color.White) }
        }
    }
}