// FILE: FeedScoopApp.kt
package ph.edu.auf.emman.yalung.feedscoop

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FeedScoopApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // setPersistenceEnabled MUST be called before any FirebaseDatabase usage.
        // Calling it here in Application.onCreate() guarantees that.
        // This enables offline disk caching — writes/reads work even without internet.
        try {
            FirebaseDatabase.getInstance(
                "https://scoopsense-5fd1e-default-rtdb.asia-southeast1.firebasedatabase.app"
            ).setPersistenceEnabled(true)
            Log.d("FeedScoopApp", "Firebase offline persistence enabled")
        } catch (e: Exception) {
            Log.w("FeedScoopApp", "setPersistenceEnabled skipped: ${e.message}")
        }
    }
}