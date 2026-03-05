// File: ui/viewmodel/ProductViewModel.kt
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

/**
 * ProductViewModel mirrors InventoryViewModel but is used in screens that need
 * the product list independently (e.g., OrderProcessingScreen).
 * Both view models share the same ProductRepository singleton.
 */
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<InventoryItem>>(emptyList())
    val products: StateFlow<List<InventoryItem>> = _products.asStateFlow()

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            repository.getAllProducts().collectLatest { list ->
                _products.value = list
            }
        }
    }

    fun updateProduct(item: InventoryItem) {
        viewModelScope.launch {
            repository.updateProduct(item)
        }
    }

    fun addProduct(item: InventoryItem) {
        viewModelScope.launch {
            repository.addProduct(item)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
    }
}