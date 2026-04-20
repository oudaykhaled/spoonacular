package nl.ing.assessment.recipes.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 2.dp,
    val small: Dp = 4.dp,
    val medium: Dp = 8.dp,
    val large: Dp = 12.dp,
    val extraLarge: Dp = 16.dp,
    val xxLarge: Dp = 20.dp,
    val xxxLarge: Dp = 24.dp,
    val huge: Dp = 64.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
