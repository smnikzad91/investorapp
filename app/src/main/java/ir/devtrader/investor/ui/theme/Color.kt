package ir.devtrader.investor.ui.theme

import androidx.compose.ui.graphics.Color

val BrandGreen = Color(0xFF00E5A0)
val BrandGreenDark = Color(0xFF00B37D)
val LossRed = Color(0xFFE53935)
val WarningAmber = Color(0xFFF5A623)
val SurfaceDark = Color(0xFF12141A)
val SurfaceDarkAlt = Color(0xFF1B1E27)
val OnSurfaceDark = Color(0xFFEAEAF0)
val MutedText = Color(0xFF8B8FA3)

/** Matches @color/ic_launcher_background exactly — the launcher icon's background, reused as the
 *  native splash theme's background and for the About screen's brand hero image. */
val SplashBackground = Color(0xFF0F1115)

// Ambient background gradient (see ui/theme/Gradients.kt) — deep navy -> teal mesh in dark mode,
// a soft low-saturation wash in light mode so contrast against card content stays high.
val GradientDarkStart = Color(0xFF0A0E1A)
val GradientDarkMid = Color(0xFF0F2733)
val GradientDarkEnd = Color(0xFF0F1115)
val GradientLightStart = Color(0xFFF2FAF7)
val GradientLightMid = Color(0xFFEEF4FB)
val GradientLightEnd = Color(0xFFFFFFFF)

// Sidebar/drawer gradient — a touch more saturated than the main background for depth separation.
val SidebarGradientDarkStart = Color(0xFF17222E)
val SidebarGradientDarkEnd = Color(0xFF0B0F14)
val SidebarGradientLightStart = Color(0xFFE6F5EF)
val SidebarGradientLightEnd = Color(0xFFFCFEFD)
