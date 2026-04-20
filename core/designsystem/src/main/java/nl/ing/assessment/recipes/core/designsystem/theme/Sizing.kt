package nl.ing.assessment.recipes.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Sizing(
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 64.dp,
    val strokeThin: Dp = 2.dp,
    val elevationSmall: Dp = 2.dp,
    val elevationMedium: Dp = 4.dp,
    val recipeImageHeight: Dp = 180.dp,
)

val LocalSizing = staticCompositionLocalOf { Sizing() }
