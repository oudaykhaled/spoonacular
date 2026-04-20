package nl.ing.assessment.recipes.core.domain.repository

import kotlinx.coroutines.flow.Flow
import nl.ing.assessment.recipes.core.domain.model.ThemeMode

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val dynamicColor: Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
}
