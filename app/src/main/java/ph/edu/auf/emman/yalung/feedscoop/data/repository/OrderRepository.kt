// FILE: data/repository/OrderRepository.kt
package ph.edu.auf.emman.yalung.feedscoop.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ph.edu.auf.emman.yalung.feedscoop.data.model.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    // All orders live under /orders/<orderId>
    private val ref = database.getReference("orders")

    /** Real-time stream of ALL orders, sorted oldest → newest by timestamp. */
    fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { child ->
                    child.getValue(Order::class.java)?.copy(id = child.key ?: "")
                }.sortedBy { it.timestamp }
                trySend(orders)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * One-shot fetch of orders within a timestamp range.
     * Uses orderByChild("timestamp") which requires the index rule in Firebase console:
     *   "orders": { ".indexOn": ["timestamp"] }
     */
    suspend fun getOrdersByDateRange(start: Long, end: Long): List<Order> {
        val snapshot = ref
            .orderByChild("timestamp")
            .startAt(start.toDouble())
            .endAt(end.toDouble())
            .get()
            .await()
        return snapshot.children.mapNotNull { child ->
            child.getValue(Order::class.java)?.copy(id = child.key ?: "")
        }.sortedBy { it.timestamp }
    }

    /** Push a new order and return its auto-generated key. */
    suspend fun addOrder(order: Order): String {
        val newRef = ref.push()
        val key = newRef.key ?: ""
        // Always stamp with current time — Order.timestamp default is fine
        // but we ensure it's set on save
        val toSave = order.copy(
            id = key,
            timestamp = System.currentTimeMillis()
        )
        newRef.setValue(toSave).await()
        return key
    }
}