// File: ui/screens/SplashScreen.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // Launch a coroutine for delay
    LaunchedEffect(key1 = true) {
        delay(2000L) // 2 seconds splash
        navController.navigate("dashboard") {
            popUpTo("splash_screen") { inclusive = true } // Remove splash from backstack
        }
    }

    // Splash UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32)), // Dark green
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FeedScoop",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Efficient feed ordering",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}