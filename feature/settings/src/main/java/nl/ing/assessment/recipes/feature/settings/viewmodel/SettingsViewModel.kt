package nl.ing.assessment.recipes.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.ing.assessment.recipes.core.domain.model.ThemeMode
import nl.ing.assessment.recipes.core.domain.repository.SettingsRepository
import nl.ing.assessment.recipes.core.telemetry.EventTracker

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val eventTracker: EventTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        eventTracker.trackScreenView("SettingsScreen")
        repository.themeMode
            .onEach { mode -> _state.update { it.copy(themeMode = mode) } }
            .launchIn(viewModelScope)
        repository.dynamicColor
            .onEach { enabled -> _state.update { it.copy(dynamicColorEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun onDynamicColorToggled(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }
}
