package ir.devtrader.investor.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Placeholder shaped like DashboardContent's real layout (status banner, wallet card, performance
 * card), shown while the first GET /dashboard call is in flight — reads as "the real screen is
 * about to appear right here" instead of a generic spinner or an unrelated screen taking over.
 */
@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dashboardSkeleton")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonPulse",
    )
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = pulse * 0.12f)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
    ) {
        item { SkeletonBlock(color = color, height = 76.dp) } // account status banner
        item { SkeletonBlock(color = color, height = 26.dp, width = 220.dp) } // "Welcome back, ..."
        item {
            SkeletonCard {
                repeat(4) { SkeletonBlock(color = color, height = 16.dp) }
            }
        }
        item {
            SkeletonCard {
                repeat(5) { SkeletonBlock(color = color, height = 16.dp) }
            }
        }
    }
}

@Composable
private fun SkeletonCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = { content() },
    )
}

@Composable
private fun SkeletonBlock(color: Color, height: Dp, width: Dp? = null) {
    Box(
        modifier = (if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .background(color, RoundedCornerShape(6.dp)),
    )
}
