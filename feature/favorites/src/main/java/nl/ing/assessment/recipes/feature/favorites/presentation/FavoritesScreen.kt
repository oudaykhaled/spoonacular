package nl.ing.assessment.recipes.feature.favorites.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import nl.ing.assessment.recipes.core.designsystem.component.EmptyState
import nl.ing.assessment.recipes.core.designsystem.component.ErrorBanner
import nl.ing.assessment.recipes.core.designsystem.component.LoadingState
import nl.ing.assessment.recipes.core.designsystem.component.RecipeCard
import nl.ing.assessment.recipes.core.designsystem.theme.spacing
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.feature.favorites.R
import nl.ing.assessment.recipes.feature.favorites.viewmodel.FavoritesUiState
import nl.ing.assessment.recipes.core.designsystem.R as DesignSystemR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onRecipeClicked: (Int) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.favorites_title)) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.error?.let { error ->
                ErrorBanner(
                    message = error,
                    onDismiss = onDismissError,
                )
            }
            FavoritesContent(
                state = state,
                onRecipeClicked = onRecipeClicked,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun FavoritesContent(
    state: FavoritesUiState,
    onRecipeClicked: (Int) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> LoadingState(modifier = modifier)
        state.favorites.isEmpty() -> EmptyState(
            title = UiText.Resource(DesignSystemR.string.empty_favorites_title),
            subtitle = UiText.Resource(DesignSystemR.string.empty_favorites_subtitle),
            modifier = modifier,
        )
        else -> FavoritesList(
            favorites = state.favorites,
            onRecipeClicked = onRecipeClicked,
            onToggleFavorite = onToggleFavorite,
            modifier = modifier,
        )
    }
}

@Composable
private fun FavoritesList(
    favorites: kotlinx.collections.immutable.ImmutableList<Recipe>,
    onRecipeClicked: (Int) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag("favorites_list"),
        contentPadding = PaddingValues(MaterialTheme.spacing.extraLarge),
    ) {
        items(
            items = favorites,
            key = { it.id },
            contentType = { "favorite_card" },
        ) { recipe ->
            RecipeCard(
                recipe = recipe,
                onClick = { onRecipeClicked(recipe.id) },
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier
                    .padding(bottom = MaterialTheme.spacing.medium)
                    .testTag("favorite_card_${recipe.id}"),
            )
        }
    }
}
