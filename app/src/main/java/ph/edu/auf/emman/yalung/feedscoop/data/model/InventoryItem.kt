// File: data/model/InventoryItem.kt
package ph.edu.auf.emman.yalung.feedscoop.data.model

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