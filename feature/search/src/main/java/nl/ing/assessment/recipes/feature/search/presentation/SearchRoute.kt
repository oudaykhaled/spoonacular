package nl.ing.assessment.recipes.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ing.assessment.recipes.core.designsystem.util.asString
import nl.ing.assessment.recipes.feature.search.viewmodel.SearchSideEffect
import nl.ing.assessment.recipes.feature.search.viewmodel.SearchViewModel

@Composable
fun SearchRoute(
    onNavigateToDetails: (Int) -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigateToDetails)
    val currentOnSnackbar by rememberUpdatedState(onShowSnackbar)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SearchSideEffect.NavigateToDetails -> currentOnNavigate(effect.recipeId)
                is SearchSideEffect.ShowSnackbar -> currentOnSnackbar(effect.message.asString(context))
            }
        }
    }

    SearchScreen(
        state = state,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSortChanged = viewModel::onSortChanged,
        onRefresh = viewModel::refresh,
        onLoadNextPage = viewModel::loadNextPage,
        onRecipeClicked = viewModel::onRecipeClicked,
        onToggleFavorite = viewModel::toggleFavorite,
        onDismissError = viewModel::dismissError,
    )
}
