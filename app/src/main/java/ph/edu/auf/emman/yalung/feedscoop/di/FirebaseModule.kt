// FILE: di/FirebaseModule.kt
package ph.edu.auf.emman.yalung.feedscoop.di

import android.util.Log
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
        // Explicitly point to the correct regional URL
        val db = FirebaseDatabase.getInstance(
            "https://scoopsense-5fd1e-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
        Log.d("FirebaseModule", "DB URL: ${db.reference}")
        return db
    }
}