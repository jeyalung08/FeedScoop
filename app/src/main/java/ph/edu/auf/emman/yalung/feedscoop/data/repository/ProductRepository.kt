// FILE: data/repository/ProductRepository.kt
package ph.edu.auf.emman.yalung.feedscoop.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ph.edu.auf.emman.yalung.feedscoop.data.model.InventoryItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    // All inventory lives under /inventory/<productId>
    private val ref = database.getReference("inventory")

    /** Real-time stream of all inventory items. Emits whenever data changes. */
    fun getAllProducts(): Flow<List<InventoryItem>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    child.getValue(InventoryItem::class.java)
                        ?.copy(productId = child.key ?: "")
                }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Push a new item; Firebase generates the key automatically. Returns the new key. */
    suspend fun addProduct(item: InventoryItem): String {
        val newRef = ref.push()
        val key = newRef.key ?: ""
        newRef.setValue(item.copy(productId = key)).await()
        return key
    }

    /** Overwrite an existing item node entirely. */
    suspend fun updateProduct(item: InventoryItem) {
        ref.child(item.productId).setValue(item).await()
    }

    /** Remove the item node by key. */
    suspend fun deleteProduct(productId: String) {
        ref.child(productId).removeValue().await()
    }

    /** Read current remainingWeight, subtract kilos, write back. */
    suspend fun deductWeight(productId: String, kilos: Double) {
        val snapshot = ref.child(productId).child("remainingWeight").get().await()
        val current = snapshot.getValue(Double::class.java) ?: 0.0
        ref.child(productId).child("remainingWeight")
            .setValue((current - kilos).coerceAtLeast(0.0)).await()
    }
}