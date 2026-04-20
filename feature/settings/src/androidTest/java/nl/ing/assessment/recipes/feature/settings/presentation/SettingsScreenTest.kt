package nl.ing.assessment.recipes.feature.settings.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.core.domain.model.ThemeMode
import nl.ing.assessment.recipes.feature.settings.viewmodel.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clicking_light_theme_option_fires_callback_with_LIGHT() {
        val selectedThemes = mutableListOf<ThemeMode>()

        composeRule.setContent {
            RecipesTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        themeMode = ThemeMode.SYSTEM,
                        dynamicColorEnabled = false,
                        dynamicColorSupported = true,
                    ),
                    onThemeModeChanged = { selectedThemes += it },
                    onDynamicColorToggled = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Light").assertIsDisplayed().performClick()

        assertEquals(listOf(ThemeMode.LIGHT), selectedThemes)
    }

    @Test
    fun clicking_dark_theme_option_fires_callback_with_DARK() {
        val selectedThemes = mutableListOf<ThemeMode>()

        composeRule.setContent {
            RecipesTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        themeMode = ThemeMode.SYSTEM,
                        dynamicColorEnabled = false,
                        dynamicColorSupported = true,
                    ),
                    onThemeModeChanged = { selectedThemes += it },
                    onDynamicColorToggled = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Dark").assertIsDisplayed().performClick()

        assertEquals(listOf(ThemeMode.DARK), selectedThemes)
    }

    @Test
    fun toggling_dynamic_color_switch_fires_callback_with_true() {
        val dynamicColorEvents = mutableListOf<Boolean>()

        composeRule.setContent {
            RecipesTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        themeMode = ThemeMode.SYSTEM,
                        dynamicColorEnabled = false,
                        dynamicColorSupported = true,
                    ),
                    onThemeModeChanged = {},
                    onDynamicColorToggled = { dynamicColorEvents += it },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Dynamic color").assertIsDisplayed()
        composeRule.onNode(isToggleable()).performClick()


        assertEquals(listOf(true), dynamicColorEvents)
    }

    @Test
    fun back_press_invokes_onBack() {
        var backInvocations = 0

        composeRule.setContent {
            RecipesTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        themeMode = ThemeMode.SYSTEM,
                        dynamicColorEnabled = false,
                        dynamicColorSupported = true,
                    ),
                    onThemeModeChanged = {},
                    onDynamicColorToggled = {},
                    onBack = { backInvocations++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()

        assertTrue(backInvocations == 1)
    }
}
