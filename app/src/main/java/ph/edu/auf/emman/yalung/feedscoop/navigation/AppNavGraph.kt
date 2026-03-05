// File: navigation/AppNavGraph.kt
package ph.edu.auf.emman.yalung.feedscoop.navigation

// AppNavGraph.kt is intentionally kept as a thin wrapper so any existing
// references to AppNavGraph still compile. All routing is handled by
// FeedScoopNavGraph. Do NOT add composable routes here to avoid duplicates.

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun AppNavGraph(navController: NavHostController) {
    FeedScoopNavGraph(navController = navController)
}