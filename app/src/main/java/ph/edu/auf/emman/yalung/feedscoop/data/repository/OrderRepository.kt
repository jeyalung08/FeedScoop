// FILE: data/repository/OrderRepository.kt
package ph.edu.auf.emman.yalung.feedscoop.data.repository

import android.util.Log
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

private const val TAG = "OrderRepo"

@Singleton
class OrderRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val ref = database.getReference("orders")

    // Manual field mapping — same approach as ProductRepository to avoid
    // reflection deserialization failures (Long vs Double, missing fields, etc.)
    private fun DataSnapshot.toOrder(): Order? {
        val id           = key ?: return null
        val productId    = child("productId").getValue(String::class.java) ?: ""
        val productName  = child("productName").getValue(String::class.java) ?: ""
        val brand        = child("brand").getValue(String::class.java) ?: ""
        val kilosOrdered = child("kilosOrdered").getValue(Double::class.java) ?: 0.0
        val pricePerKilo = child("pricePerKilo").getValue(Double::class.java) ?: 0.0
        val totalPrice   = child("totalPrice").getValue(Double::class.java) ?: 0.0
        // Firebase stores Long — coerce to Long safely
        val timestamp    = child("timestamp").getValue(Long::class.java)
            ?: child("timestamp").getValue(Double::class.java)?.toLong()
            ?: System.currentTimeMillis()
        return Order(
            id           = id,
            productId    = productId,
            productName  = productName,
            brand        = brand,
            kilosOrdered = kilosOrdered,
            pricePerKilo = pricePerKilo,
            totalPrice   = totalPrice,
            timestamp    = timestamp
        )
    }

    /** Real-time stream of ALL orders, sorted oldest → newest. */
    fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "getAllOrders: attaching listener")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: children=${snapshot.childrenCount}")
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .sortedBy { it.timestamp }
                trySend(orders)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** One-shot fetch filtered by timestamp range. Requires .indexOn ["timestamp"] in rules. */
    suspend fun getOrdersByDateRange(start: Long, end: Long): List<Order> {
        val snapshot = ref
            .orderByChild("timestamp")
            .startAt(start.toDouble())
            .endAt(end.toDouble())
            .get()
            .await()
        return snapshot.children.mapNotNull { it.toOrder() }
            .sortedBy { it.timestamp }
    }

    /** Write a new order as a plain map — no reflection, no type ambiguity. */
    fun addOrder(order: Order): String {
        val newRef = ref.push()
        val key    = newRef.key ?: ""
        val now    = System.currentTimeMillis()
        val map    = mapOf(
            "id"           to key,
            "productId"    to order.productId,
            "productName"  to order.productName,
            "brand"        to order.brand,
            "kilosOrdered" to order.kilosOrdered,
            "pricePerKilo" to order.pricePerKilo,
            "totalPrice"   to order.totalPrice,
            "timestamp"    to now
        )
        newRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "addOrder FAILED: ${error.message}")
            else Log.d(TAG, "addOrder SUCCESS key=$key")
        }
        return key
    }
}