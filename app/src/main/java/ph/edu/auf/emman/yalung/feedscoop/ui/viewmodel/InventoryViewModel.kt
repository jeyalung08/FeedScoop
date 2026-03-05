// File: ui/viewmodel/InventoryViewModel.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ph.edu.auf.emman.yalung.feedscoop.data.model.InventoryItem
import ph.edu.auf.emman.yalung.feedscoop.data.repository.ProductRepository
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    init {
        fetchInventory()
    }

    private fun fetchInventory() {
        viewModelScope.launch {
            repository.getAllProducts().collectLatest { items ->
                _inventoryItems.value = items
            }
        }
    }

    fun addProduct(item: InventoryItem) {
        viewModelScope.launch {
            repository.addProduct(item)
        }
    }

    fun updateProduct(item: InventoryItem) {
        viewModelScope.launch {
            repository.updateProduct(item)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
    }

    fun deductWeight(productId: String, kilos: Double) {
        viewModelScope.launch {
            repository.deductWeight(productId, kilos)
        }
    }
}