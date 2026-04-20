package nl.ing.assessment.recipes.core.designsystem.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

/**
 * Renders a composable in both light and dark theme in Android Studio Preview.
 * Usage: annotate any stateless *Screen composable with @LightDarkPreview.
 */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
annotation class LightDarkPreview
