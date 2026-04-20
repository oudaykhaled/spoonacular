package nl.ing.assessment.recipes.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ing.assessment.recipes.feature.settings.viewmodel.SettingsViewModel

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onThemeModeChanged = viewModel::onThemeModeChanged,
        onDynamicColorToggled = viewModel::onDynamicColorToggled,
        onBack = onBack,
        modifier = modifier,
    )
}
