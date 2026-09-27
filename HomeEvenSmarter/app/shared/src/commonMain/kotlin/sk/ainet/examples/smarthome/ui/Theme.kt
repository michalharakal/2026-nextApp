package sk.ainet.examples.smarthome.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** A dark, high-contrast palette that reads on a projector. */
object Palette {
    val background = Color(0xFF0F1115)
    val surface = Color(0xFF181B22)
    val surfaceHigh = Color(0xFF232730)
    val outline = Color(0xFF3A3F4B)
    val text = Color(0xFFECEFF4)
    val textDim = Color(0xFF9AA3B2)
    val accent = Color(0xFF4FC3F7)      // ASR / listening
    val accent2 = Color(0xFFB388FF)     // NLU
    val ok = Color(0xFF66BB6A)          // action done
    val warn = Color(0xFFFFB74D)        // warm-up, pending
    val error = Color(0xFFEF5350)
    val lightOn = Color(0xFFFFD54F)
    val cloud = Color(0xFF546E7A)
}

private val scheme = darkColorScheme(
    primary = Palette.accent,
    secondary = Palette.accent2,
    tertiary = Palette.ok,
    background = Palette.background,
    surface = Palette.surface,
    surfaceVariant = Palette.surfaceHigh,
    onPrimary = Palette.background,
    onBackground = Palette.text,
    onSurface = Palette.text,
    onSurfaceVariant = Palette.textDim,
    outline = Palette.outline,
    error = Palette.error,
)

@Composable
fun HomeEvenSmarterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
