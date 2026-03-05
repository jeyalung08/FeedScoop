// File: data/model/Product.kt
package ph.edu.auf.emman.yalung.feedscoop.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val type: String = "",
    val netWeight: Double = 0.0,
    val pricePerKilo: Double = 0.0,
    val imageUrl: String = ""
)