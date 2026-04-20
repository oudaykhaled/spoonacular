package nl.ing.assessment.recipes.feature.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import nl.ing.assessment.recipes.core.designsystem.component.EmptyState
import nl.ing.assessment.recipes.core.designsystem.component.ErrorState
import nl.ing.assessment.recipes.core.designsystem.component.LoadingState
import nl.ing.assessment.recipes.core.designsystem.component.RecipeCard
import nl.ing.assessment.recipes.core.designsystem.component.SearchBar
import nl.ing.assessment.recipes.core.designsystem.component.SortChipsRow
import nl.ing.assessment.recipes.core.designsystem.theme.spacing
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.core.designsystem.preview.LightDarkPreview
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.feature.search.R
import nl.ing.assessment.recipes.feature.search.viewmodel.SearchUiState
import kotlinx.collections.immutable.persistentListOf

private const val PREFETCH_THRESHOLD = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSortChanged: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onRecipeClicked: (Int) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = state.searchInput,
            onQueryChange = onSearchQueryChanged,
            onClear = { onSearchQueryChanged("") },
            placeholder = stringResource(R.string.search_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.extraLarge,
                    vertical = MaterialTheme.spacing.medium,
                )
                .testTag("search_bar"),
        )

        SortChipsRow(
            selected = state.sort,
            onSelect = onSortChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.small)
                .testTag("sort_chips"),
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            SearchContent(
                state = state,
                onLoadNextPage = onLoadNextPage,
                onRecipeClicked = onRecipeClicked,
                onToggleFavorite = onToggleFavorite,
                onRetry = onRefresh,
                onDismissError = onDismissError,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SearchContent(
    state: SearchUiState,
    onLoadNextPage: () -> Unit,
    onRecipeClicked: (Int) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading && state.recipes.isEmpty() -> LoadingState(modifier = modifier)

        state.error != null && state.recipes.isEmpty() -> ErrorState(
            message = state.error,
            onRetry = {
                // Dismiss error state before retrying so the loading indicator shows immediately.
                onDismissError()
                onRetry()
            },
            modifier = modifier,
        )

        state.recipes.isEmpty() && state.searchQuery.isBlank() -> EmptyState(
            title = UiText.Resource(R.string.search_empty_title),
            subtitle = UiText.Resource(R.string.search_empty_subtitle),
            modifier = modifier,
        )

        state.recipes.isEmpty() -> EmptyState(
            title = UiText.Resource(R.string.no_results_title),
            subtitle = UiText.Resource(R.string.no_results_subtitle),
            modifier = modifier,
        )

        else -> RecipesList(
            state = state,
            onLoadNextPage = onLoadNextPage,
            onRecipeClicked = onRecipeClicked,
            onToggleFavorite = onToggleFavorite,
            modifier = modifier,
        )
    }
}

@Composable
private fun RecipesList(
    state: SearchUiState,
    onLoadNextPage: () -> Unit,
    onRecipeClicked: (Int) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.hasMorePages) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to total
        }
            .distinctUntilChanged()
            .filter { (lastVisible, total) ->
                total > 0 && lastVisible >= total - PREFETCH_THRESHOLD && state.hasMorePages
            }
            .collect { onLoadNextPage() }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(MaterialTheme.spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = modifier.testTag("search_list"),
    ) {
        items(
            items = state.recipes,
            key = { it.id },
            contentType = { "recipe" },
        ) { recipe ->
            RecipeCard(
                recipe = recipe,
                onClick = { onRecipeClicked(recipe.id) },
                onToggleFavorite = onToggleFavorite,
            )
        }

        if (state.isLoadingMore) {
            item(key = "loading_more", contentType = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.extraLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun SearchScreenPreview() {
    RecipesTheme {
        SearchScreen(
            state = SearchUiState(
                recipes = persistentListOf(),
                isLoading = false,
            ),
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
