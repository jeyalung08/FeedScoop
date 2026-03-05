package ph.edu.auf.emman.yalung.feedscoop.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenLight = Color(0xFFB2FF59)
private val GreenPrimary = Color(0xFF4CAF50)
private val GreenDark = Color(0xFF087F23)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    secondary = GreenLight,
    onSecondary = Color.Black,
    background = Color(0xFFF0FFF0),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

@Composable
fun FeedScoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}