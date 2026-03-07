// FILE: ui/viewmodel/OrderViewModel.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ph.edu.auf.emman.yalung.feedscoop.data.model.Order
import ph.edu.auf.emman.yalung.feedscoop.data.repository.OrderRepository
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {

    // In-progress orders for the current checkout session
    private val _currentOrders = MutableStateFlow<List<Order>>(emptyList())
    val currentOrders: StateFlow<List<Order>> = _currentOrders.asStateFlow()

    // Orders loaded from Firebase (history / analytics)
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllOrders().collectLatest { list ->
                _orders.value = list
            }
        }
    }

    /** Add order to session and persist to Firebase — fire-and-forget write */
    fun addOrder(order: Order, inventoryViewModel: InventoryViewModel? = null) {
        val key   = repository.addOrder(order)           // non-suspend, instant
        val saved = order.copy(id = key)
        _currentOrders.value = _currentOrders.value + saved
        inventoryViewModel?.deductWeight(order.productId, order.kilosOrdered)
    }

    fun loadOrdersByDateRange(startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            _orders.value = repository.getOrdersByDateRange(startMillis, endMillis)
        }
    }

    fun loadAllOrders() {
        viewModelScope.launch {
            repository.getAllOrders().collectLatest { list ->
                _orders.value = list
            }
        }
    }

    fun clearCurrentOrders() {
        _currentOrders.value = emptyList()
    }

    fun removeOrder(order: Order) {
        _currentOrders.value = _currentOrders.value.filter { it != order }
    }
}