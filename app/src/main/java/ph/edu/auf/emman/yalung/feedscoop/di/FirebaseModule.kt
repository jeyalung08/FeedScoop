// FILE: di/FirebaseModule.kt
package ph.edu.auf.emman.yalung.feedscoop.di

import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance().also {
            // Enables offline disk persistence — data survives app restarts without internet
            it.setPersistenceEnabled(true)
        }
    }
}