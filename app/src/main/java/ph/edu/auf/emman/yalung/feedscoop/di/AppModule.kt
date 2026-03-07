// FILE: di/AppModule.kt
package ph.edu.auf.emman.yalung.feedscoop.di

import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ph.edu.auf.emman.yalung.feedscoop.data.repository.OrderRepository
import ph.edu.auf.emman.yalung.feedscoop.data.repository.ProductRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideProductRepository(database: FirebaseDatabase): ProductRepository =
        ProductRepository(database)

    @Provides
    @Singleton
    fun provideOrderRepository(database: FirebaseDatabase): OrderRepository =
        OrderRepository(database)

    // DeviceViewModel now also uses FirebaseDatabase (injected via @Inject constructor)
    // No manual provider needed — Hilt handles it automatically since
    // DeviceViewModel is @HiltViewModel and FirebaseDatabase is @Singleton above.
}