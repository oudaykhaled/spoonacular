package nl.ing.assessment.recipes.feature.favorites.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.collections.immutable.persistentListOf
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.feature.favorites.viewmodel.FavoritesUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun recipe(id: Int, title: String = "Recipe $id"): Recipe = Recipe(
        id = id,
        title = title,
        image = null,
        imageType = null,
        summary = null,
        readyInMinutes = null,
        healthScore = null,
        aggregateLikes = null,
        pricePerServing = null,
        sourceUrl = null,
        isFavorite = true,
        cachedAt = 0L,
    )

    @Test
    fun renders_list_with_favorites_list_tag() {
        val state = FavoritesUiState(
            favorites = persistentListOf(recipe(1), recipe(2)),
            isLoading = false,
        )

        composeRule.setContent {
            RecipesTheme {
                FavoritesScreen(
                    state = state,
                    onRecipeClicked = {},
                    onToggleFavorite = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithTag("favorites_list").assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_card_1").assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_card_2").assertIsDisplayed()
    }

    @Test
    fun empty_state_when_no_favorites_and_not_loading() {
        val state = FavoritesUiState(
            favorites = persistentListOf(),
            isLoading = false,
        )

        composeRule.setContent {
            RecipesTheme {
                FavoritesScreen(
                    state = state,
                    onRecipeClicked = {},
                    onToggleFavorite = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithText("No favorites yet").assertIsDisplayed()
    }

    @Test
    fun error_banner_displays_and_dismiss_callback_fires() {
        var dismissCount = 0
        val state = FavoritesUiState(
            favorites = persistentListOf(recipe(1)),
            isLoading = false,
            error = UiText.Raw("boom"),
        )

        composeRule.setContent {
            RecipesTheme {
                FavoritesScreen(
                    state = state,
                    onRecipeClicked = {},
                    onToggleFavorite = {},
                    onDismissError = { dismissCount++ },
                )
            }
        }

        composeRule.onNodeWithTag("error_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("error_banner_dismiss").performClick()

        assertEquals(1, dismissCount)
    }

    @Test
    fun clicking_recipe_card_fires_callback() {
        val clickedIds = mutableListOf<Int>()
        val state = FavoritesUiState(
            favorites = persistentListOf(recipe(42, title = "Meaning of Life Stew")),
            isLoading = false,
        )

        composeRule.setContent {
            RecipesTheme {
                FavoritesScreen(
                    state = state,
                    onRecipeClicked = { clickedIds += it },
                    onToggleFavorite = {},
                    onDismissError = {},
                )
            }
        }

        composeRule.onNodeWithTag("favorite_card_42").performClick()

        assertEquals(listOf(42), clickedIds)
    }
}
