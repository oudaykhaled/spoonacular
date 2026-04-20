package nl.ing.assessment.recipes.feature.search.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.feature.search.viewmodel.SearchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendering_populated_list_displays_search_list_tag() {
        val state = baseState().copy(recipes = fakeRecipes(3))

        composeRule.setContent {
            RecipesTheme {
                SearchScreen(
                    state = state,
                    onSearchQueryChanged = {},
                    onSortChanged = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRecipeClicked = {},
                    onToggleFavorite = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithTag("search_list").assertIsDisplayed()
    }

    @Test
    fun initial_empty_state_shows_search_empty_title() {
        val state = baseState().copy(
            recipes = persistentListOf(),
            searchQuery = "",
        )

        composeRule.setContent {
            RecipesTheme {
                SearchScreen(
                    state = state,
                    onSearchQueryChanged = {},
                    onSortChanged = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRecipeClicked = {},
                    onToggleFavorite = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithText(SEARCH_EMPTY_TITLE).assertIsDisplayed()
    }

    @Test
    fun no_results_state_shows_no_results_title() {
        val state = baseState().copy(
            recipes = persistentListOf(),
            searchQuery = "pasta",
        )

        composeRule.setContent {
            RecipesTheme {
                SearchScreen(
                    state = state,
                    onSearchQueryChanged = {},
                    onSortChanged = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRecipeClicked = {},
                    onToggleFavorite = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithText(NO_RESULTS_TITLE).assertIsDisplayed()
    }

    @Test
    fun error_state_triggers_retry_callback() {
        val state = baseState().copy(
            recipes = persistentListOf(),
            error = UiText.Raw("boom"),
        )
        var refreshCount = 0
        var dismissErrorCount = 0

        composeRule.setContent {
            RecipesTheme {
                SearchScreen(
                    state = state,
                    onSearchQueryChanged = {},
                    onSortChanged = {},
                    onRefresh = { refreshCount++ },
                    onLoadNextPage = {},
                    onRecipeClicked = {},
                    onToggleFavorite = {},
                    onDismissError = { dismissErrorCount++ },
                )
            }
        }

        composeRule.onNodeWithText(RETRY_LABEL).performClick()

        assertEquals(1, refreshCount)
        assertEquals(1, dismissErrorCount)
    }

    @Test
    fun typing_in_search_bar_fires_onSearchQueryChanged() {
        val captured = mutableListOf<String>()

        composeRule.setContent {
            RecipesTheme {
                SearchScreen(
                    state = baseState(),
                    onSearchQueryChanged = { captured += it },
                    onSortChanged = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onRecipeClicked = {},
                    onToggleFavorite = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithTag("search_bar").performTextInput("pizza")

        assertTrue(
            "Expected captured typed chars to contain \"pizza\" in order, got $captured",
            captured.joinToString(separator = "").contains("pizza"),
        )
    }

    private fun baseState(): SearchUiState = SearchUiState(sort = SortOrder.RELEVANCE)

    private fun fakeRecipes(n: Int): ImmutableList<Recipe> = (1..n).map { id ->
        Recipe(
            id = id,
            title = "Recipe $id",
            image = null,
            imageType = null,
            summary = "desc $id",
            readyInMinutes = 30,
            healthScore = 50.0,
            aggregateLikes = 10,
            pricePerServing = 100.0,
            sourceUrl = null,
            isFavorite = false,
            cachedAt = 0L,
        )
    }.toImmutableList()

    private companion object {
        const val SEARCH_EMPTY_TITLE = "Find your next recipe"
        const val NO_RESULTS_TITLE = "No recipes found"
        const val RETRY_LABEL = "Retry"
    }
}
