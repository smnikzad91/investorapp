package ir.devtrader.investor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

/**
 * Ambient background for the app's main canvas (behind every screen's own opaque cards/surfaces —
 * only shows through padding/gaps and app-bar/scaffold areas made transparent for this purpose,
 * see MainActivity/AppShell/LoginScreen/RegisterScreen). A diagonal multi-stop gradient reads as
 * a soft mesh without needing a real multi-point mesh-gradient API.
 */
@Composable
fun appBackgroundBrush(): Brush = if (isSystemInDarkTheme()) {
    Brush.linearGradient(listOf(GradientDarkStart, GradientDarkMid, GradientDarkEnd))
} else {
    Brush.linearGradient(listOf(GradientLightStart, GradientLightMid, GradientLightEnd))
}

/** Elevated gradient for the nav drawer sheet — distinct from, but cohesive with, the main background. */
@Composable
fun sidebarBrush(): Brush = if (isSystemInDarkTheme()) {
    Brush.verticalGradient(listOf(SidebarGradientDarkStart, SidebarGradientDarkEnd))
} else {
    Brush.verticalGradient(listOf(SidebarGradientLightStart, SidebarGradientLightEnd))
}
