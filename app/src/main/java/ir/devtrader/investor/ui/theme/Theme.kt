package ir.devtrader.investor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = SurfaceDark,
    secondary = BrandGreenDark,
    background = SurfaceDark,
    surface = SurfaceDarkAlt,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    error = LossRed,
)

private val LightColors = lightColorScheme(
    primary = BrandGreenDark,
    secondary = BrandGreen,
    error = LossRed,
)

@Composable
fun TradeBotInvestorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
