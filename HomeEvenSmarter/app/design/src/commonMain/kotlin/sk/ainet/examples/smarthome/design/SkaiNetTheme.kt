package sk.ainet.examples.smarthome.design

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * SKaiNET brand tokens. The two brand colors are sampled from the project logo — red `#C33A32`, node
 * charcoal `#3B403C` — and the neutrals carry the charcoal's slight green cast so the dark surfaces feel
 * like the logo mark, not plain grey. Dark and high-contrast on purpose: it has to read on a projector.
 */
object SkaiNet {
    // brand
    val red = Color(0xFFC33A32)
    val redBright = Color(0xFFE0564C)
    val charcoal = Color(0xFF3B403C)

    // neutrals (charcoal-tinted)
    val background = Color(0xFF121413)
    val surface = Color(0xFF1B1E1C)
    val surfaceHigh = Color(0xFF262A27)
    val outline = Color(0xFF454B46)
    val text = Color(0xFFF3F4F2)
    val textDim = Color(0xFFA9B0AA)

    // functional (stage and status colors; distinct from the brand red on purpose)
    val nlu = Color(0xFFB388FF)
    val ok = Color(0xFF66BB6A)
    val warn = Color(0xFFFFB74D)
    val error = Color(0xFFFF5252)
    val lightOn = Color(0xFFFFD54F)
    val cloud = Color(0xFF5E6E79)
}

private val scheme = darkColorScheme(
    primary = SkaiNet.red,
    onPrimary = SkaiNet.text,
    secondary = SkaiNet.nlu,
    tertiary = SkaiNet.ok,
    background = SkaiNet.background,
    surface = SkaiNet.surface,
    surfaceVariant = SkaiNet.surfaceHigh,
    secondaryContainer = SkaiNet.red.copy(alpha = 0.28f),
    onSecondaryContainer = SkaiNet.text,
    onBackground = SkaiNet.text,
    onSurface = SkaiNet.text,
    onSurfaceVariant = SkaiNet.textDim,
    outline = SkaiNet.outline,
    error = SkaiNet.error,
)

@Composable
fun SkaiNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme) {
        // Screens draw their own backgrounds instead of sitting in a Surface, so the default content color
        // would stay black. Pin it to the theme's text color: un-colored Text is readable on dark, always.
        CompositionLocalProvider(LocalContentColor provides SkaiNet.text, content = content)
    }
}
