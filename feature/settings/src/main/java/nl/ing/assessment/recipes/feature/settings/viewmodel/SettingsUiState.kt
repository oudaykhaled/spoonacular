package nl.ing.assessment.recipes.feature.settings.viewmodel

import android.os.Build
import androidx.compose.runtime.Immutable
import nl.ing.assessment.recipes.core.designsystem.theme.ThemeMode

@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val dynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
)
