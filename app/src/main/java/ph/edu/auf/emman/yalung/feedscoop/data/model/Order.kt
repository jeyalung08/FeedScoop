// File: data/model/Order.kt
package ph.edu.auf.emman.yalung.feedscoop.data.model

data class Order(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val brand: String = "",
    val kilosOrdered: Double = 0.0,
    val pricePerKilo: Double = 0.0,
    val totalPrice: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)