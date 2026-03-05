  // File: MainActivity.kt
package ph.edu.auf.emman.yalung.feedscoop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ph.edu.auf.emman.yalung.feedscoop.navigation.FeedScoopNavGraph
import ph.edu.auf.emman.yalung.feedscoop.ui.theme.FeedScoopTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeedScoopTheme {
                val navController = rememberNavController()
                FeedScoopNavGraph(navController = navController)
            }
        }
    }
}