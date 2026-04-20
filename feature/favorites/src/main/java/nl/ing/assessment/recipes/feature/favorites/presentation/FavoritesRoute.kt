package nl.ing.assessment.recipes.feature.favorites.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.feature.favorites.viewmodel.FavoritesSideEffect
import nl.ing.assessment.recipes.feature.favorites.viewmodel.FavoritesViewModel

@Composable
fun FavoritesRoute(
    onNavigateToDetails: (Int) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigateToDetails)
    val currentOnSnackbar by rememberUpdatedState(onShowSnackbar)
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is FavoritesSideEffect.NavigateToDetails -> currentOnNavigate(effect.recipeId)
                is FavoritesSideEffect.ShowSnackbar -> {
                    val message = when (val msg = effect.message) {
                        is UiText.Raw -> msg.value
                        is UiText.Resource -> resources.getString(msg.resId, *msg.args.toTypedArray())
                    }
                    currentOnSnackbar(message)
                }
            }
        }
    }

    FavoritesScreen(
        state = state,
        onRecipeClicked = viewModel::onRecipeClicked,
        onToggleFavorite = viewModel::toggleFavorite,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
    )
}
