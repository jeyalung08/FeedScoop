// FILE: FeedScoopApp.kt
package ph.edu.auf.emman.yalung.feedscoop

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Persistence temporarily removed to rule out caching issues.
// Re-add later once writes are confirmed working.
@HiltAndroidApp
class FeedScoopApp : Application()