package nl.ing.assessment.recipes.feature.details.presentation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import nl.ing.assessment.recipes.core.designsystem.R as DesignSystemR
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.feature.details.R
import nl.ing.assessment.recipes.feature.details.viewmodel.DetailsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_state_renders_loading_indicator() {
        val state = DetailsUiState(isLoading = true, details = null)

        composeRule.setContent {
            RecipesTheme {
                DetailsScreen(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onToggleFavorite = {},
                    onOpenSource = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithTag("details_screen").assertIsDisplayed()
        // LoadingState renders a CircularProgressIndicator which exposes a
        // ProgressBarRangeInfo semantic — asserting on the semantic is more stable
        // than relying on unlabelled testTags inside the design-system component.
        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsDisplayed()
    }

    @Test
    fun populated_state_shows_recipe_title() {
        val state = DetailsUiState(
            details = RecipeDetails(recipe = recipe(title = "Chocolate Mousse")),
            isLoading = false,
        )

        composeRule.setContent {
            RecipesTheme {
                DetailsScreen(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onToggleFavorite = {},
                    onOpenSource = {},
                    onDismissError = {},
                )
            }
        }

        // Title appears both in the TopAppBar and the DetailsHeader — assert at least one is
        // displayed rather than requiring a single match.
        composeRule
            .onAllNodesWithText("Chocolate Mousse")
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun error_state_with_no_details_renders_error_retry_and_retry_click_fires() {
        val retryCalls = mutableListOf<Unit>()
        val state = DetailsUiState(
            details = null,
            isLoading = false,
            error = UiText.Raw("boom"),
        )

        composeRule.setContent {
            RecipesTheme {
                DetailsScreen(
                    state = state,
                    onBack = {},
                    onRetry = { retryCalls += Unit },
                    onToggleFavorite = {},
                    onOpenSource = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithText("boom").assertIsDisplayed()

        val retryLabel = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(DesignSystemR.string.error_retry)
        composeRule.onNodeWithText(retryLabel).performClick()

        assertEquals(1, retryCalls.size)
    }

    @Test
    fun open_source_button_click_fires_callback() {
        val openSourceCalls = mutableListOf<Unit>()
        val state = DetailsUiState(
            details = RecipeDetails(
                recipe = recipe(
                    title = "Apple Pie",
                    sourceUrl = "https://example.com",
                ),
            ),
            isLoading = false,
        )

        composeRule.setContent {
            RecipesTheme {
                DetailsScreen(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onToggleFavorite = {},
                    onOpenSource = { openSourceCalls += Unit },
                    onDismissError = {},
                )
            }
        }

        val openSourceLabel = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.details_open_source)
        composeRule.onNodeWithText(openSourceLabel).performClick()

        assertEquals(1, openSourceCalls.size)
    }

    private fun recipe(
        title: String,
        sourceUrl: String? = null,
    ): Recipe = Recipe(
        id = 1,
        title = title,
        sourceUrl = sourceUrl,
        cachedAt = 0L,
    )
}
