// FILE: data/model/InventoryItem.kt
package ph.edu.auf.emman.yalung.feedscoop.data.model

// All fields must have default values for Firebase Realtime Database deserialization.
// Firebase uses reflection to map JSON keys to field names — names must match exactly.
// imageUrl is kept so existing Firebase data with that field doesn't cause parse errors.
data class InventoryItem(
    val productId: String = "",
    val name: String = "",
    val brand: String = "",
    val type: String = "",
    val totalWeight: Double = 0.0,
    val remainingWeight: Double = 0.0,
    val pricePerKilo: Double = 0.0,
    val imageUrl: String = ""
)