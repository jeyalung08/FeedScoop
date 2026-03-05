// FILE: ui/viewmodel/InventoryViewModel.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel

import android.util.Log
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

private const val TAG = "InventoryVM"

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private val _saveComplete = MutableStateFlow(false)
    val saveComplete: StateFlow<Boolean> = _saveComplete.asStateFlow()

    init {
        Log.d(TAG, "InventoryViewModel created")
        fetchInventory()
    }

    private fun fetchInventory() {
        Log.d(TAG, "fetchInventory: starting collection")
        viewModelScope.launch {
            repository.getAllProducts().collectLatest { items ->
                Log.d(TAG, "fetchInventory: received ${items.size} items")
                _inventoryItems.value = items
            }
        }
    }

    fun addProduct(item: InventoryItem) {
        Log.d(TAG, "addProduct: called name=${item.name}")
        repository.addProduct(item)
        Log.d(TAG, "addProduct: signalling saveComplete")
        _saveComplete.value = true
    }

    fun updateProduct(item: InventoryItem) {
        Log.d(TAG, "updateProduct: called id=${item.productId}")
        repository.updateProduct(item)
        _saveComplete.value = true
    }

    fun deleteProduct(productId: String) {
        Log.d(TAG, "deleteProduct: called id=$productId")
        repository.deleteProduct(productId)
    }

    fun deductWeight(productId: String, kilos: Double) {
        viewModelScope.launch { repository.deductWeight(productId, kilos) }
    }

    fun resetSaveComplete() {
        Log.d(TAG, "resetSaveComplete")
        _saveComplete.value = false
    }
}