package ph.edu.auf.emman.yalung.feedscoop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScoopWeightMeter(currentWeight: Float, targetWeight: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = "Weight: ${"%.2f".format(currentWeight)} / ${"%.2f".format(targetWeight)} kg")
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (currentWeight / targetWeight).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}