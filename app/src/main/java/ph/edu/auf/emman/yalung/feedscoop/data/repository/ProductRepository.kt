// FILE: data/repository/ProductRepository.kt
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
import ph.edu.auf.emman.yalung.feedscoop.data.model.InventoryItem
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProductRepo"

@Singleton
class ProductRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val ref = database.getReference("inventory")

    fun getAllProducts(): Flow<List<InventoryItem>> = callbackFlow {
        Log.d(TAG, "getAllProducts: attaching listener, ref=${ref.toString()}")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: exists=${snapshot.exists()} children=${snapshot.childrenCount}")
                val items = snapshot.children.mapNotNull { child ->
                    val productId       = child.key ?: return@mapNotNull null
                    val name            = child.child("name").getValue(String::class.java) ?: ""
                    val brand           = child.child("brand").getValue(String::class.java) ?: ""
                    val type            = child.child("type").getValue(String::class.java) ?: ""
                    val totalWeight     = child.child("totalWeight").getValue(Double::class.java) ?: 0.0
                    val remainingWeight = child.child("remainingWeight").getValue(Double::class.java) ?: 0.0
                    val pricePerKilo    = child.child("pricePerKilo").getValue(Double::class.java) ?: 0.0
                    Log.d(TAG, "  parsed: id=$productId name=$name")
                    InventoryItem(productId, name, brand, type, totalWeight, remainingWeight, pricePerKilo)
                }
                Log.d(TAG, "onDataChange: emitting ${items.size} items")
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: ${error.message} code=${error.code}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "getAllProducts: removing listener")
            ref.removeEventListener(listener)
        }
    }

    fun addProduct(item: InventoryItem): String {
        Log.d(TAG, "addProduct: called for name=${item.name}")
        val newRef = ref.push()
        val key = newRef.key ?: ""
        Log.d(TAG, "addProduct: key=$key path=${newRef.toString()}")
        val map = mapOf(
            "productId"       to key,
            "name"            to item.name,
            "brand"           to item.brand,
            "type"            to item.type,
            "totalWeight"     to item.totalWeight,
            "remainingWeight" to item.remainingWeight,
            "pricePerKilo"    to item.pricePerKilo
        )
        newRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "addProduct: write FAILED ${error.message}")
            else Log.d(TAG, "addProduct: write SUCCESS key=$key")
        }
        return key
    }

    fun updateProduct(item: InventoryItem) {
        Log.d(TAG, "updateProduct: id=${item.productId} name=${item.name}")
        val map = mapOf(
            "productId"       to item.productId,
            "name"            to item.name,
            "brand"           to item.brand,
            "type"            to item.type,
            "totalWeight"     to item.totalWeight,
            "remainingWeight" to item.remainingWeight,
            "pricePerKilo"    to item.pricePerKilo
        )
        ref.child(item.productId).setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "updateProduct: FAILED ${error.message}")
            else Log.d(TAG, "updateProduct: SUCCESS")
        }
    }

    fun deleteProduct(productId: String) {
        Log.d(TAG, "deleteProduct: id=$productId")
        ref.child(productId).removeValue() { error, _ ->
            if (error != null) Log.e(TAG, "deleteProduct: FAILED ${error.message}")
            else Log.d(TAG, "deleteProduct: SUCCESS")
        }
    }

    suspend fun deductWeight(productId: String, kilos: Double) {
        Log.d(TAG, "deductWeight: id=$productId kilos=$kilos")
        val snapshot = ref.child(productId).child("remainingWeight").get().await()
        val current = snapshot.getValue(Double::class.java) ?: 0.0
        ref.child(productId).child("remainingWeight")
            .setValue((current - kilos).coerceAtLeast(0.0))
    }
}