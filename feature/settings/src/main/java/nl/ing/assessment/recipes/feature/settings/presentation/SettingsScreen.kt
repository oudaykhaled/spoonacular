package nl.ing.assessment.recipes.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import nl.ing.assessment.recipes.core.designsystem.preview.LightDarkPreview
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.core.designsystem.theme.spacing
import nl.ing.assessment.recipes.core.domain.model.ThemeMode
import nl.ing.assessment.recipes.feature.settings.R
import nl.ing.assessment.recipes.feature.settings.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        SettingsContent(
            state = state,
            onThemeModeChanged = onThemeModeChanged,
            onDynamicColorToggled = onDynamicColorToggled,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
    ) {
        item(key = "theme-section", contentType = "section") {
            ThemeSection(
                selected = state.themeMode,
                onThemeModeChanged = onThemeModeChanged,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraLarge),
            )
        }
        item(key = "dynamic-color-section", contentType = "section") {
            DynamicColorSection(
                enabled = state.dynamicColorEnabled,
                supported = state.dynamicColorSupported,
                onDynamicColorToggled = onDynamicColorToggled,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraLarge),
            )
        }
        item(key = "about-section", contentType = "section") {
            AboutSection(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraLarge),
            )
        }
    }
}

@Composable
private fun ThemeSection(
    selected: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text(
                text = stringResource(R.string.settings_theme_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Column(modifier = Modifier.selectableGroup()) {
                ThemeOption(
                    label = stringResource(R.string.settings_theme_system),
                    selected = selected == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChanged(ThemeMode.SYSTEM) },
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_light),
                    selected = selected == ThemeMode.LIGHT,
                    onClick = { onThemeModeChanged(ThemeMode.LIGHT) },
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = selected == ThemeMode.DARK,
                    onClick = { onThemeModeChanged(ThemeMode.DARK) },
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = MaterialTheme.spacing.medium),
        )
    }
}

@Composable
private fun DynamicColorSection(
    enabled: Boolean,
    supported: Boolean,
    onDynamicColorToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_dynamic_color),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled && supported,
                    onCheckedChange = onDynamicColorToggled,
                    enabled = supported,
                )
            }
            if (!supported) {
                Text(
                    text = stringResource(R.string.settings_dynamic_color_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutSection(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@LightDarkPreview
@Suppress("UnusedPrivateMember") // Preview is used by Android Studio; not a runtime caller
@Composable
private fun SettingsScreenPreview() {
    RecipesTheme {
        SettingsScreen(
            state = SettingsUiState(),
            onThemeModeChanged = {},
            onDynamicColorToggled = {},
            onBack = {},
        )
    }
}
