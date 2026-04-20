package nl.ing.assessment.recipes.feature.details.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.feature.details.R
import nl.ing.assessment.recipes.feature.details.viewmodel.DetailsSideEffect
import nl.ing.assessment.recipes.feature.details.viewmodel.DetailsViewModel

@Composable
fun DetailsRoute(
    recipeId: Int,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(
        creationCallback = { factory -> factory.create(recipeId) }
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnSnackbar by rememberUpdatedState(onShowSnackbar)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is DetailsSideEffect.OpenUrl -> {
                    runCatching {
                        val uri = Uri.parse(effect.url)
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        currentOnSnackbar(context.getString(R.string.details_error_open_url))
                    }
                }
                is DetailsSideEffect.ShowSnackbar -> {
                    val message = when (val msg = effect.message) {
                        is UiText.Raw -> msg.value
                        is UiText.Resource -> context.getString(msg.resId, *msg.args.toTypedArray())
                    }
                    currentOnSnackbar(message)
                }
            }
        }
    }

    DetailsScreen(
        state = state,
        onBack = currentOnBack,
        onRetry = viewModel::retry,
        onToggleFavorite = viewModel::toggleFavorite,
        onOpenSource = viewModel::onOpenSource,
        onDismissError = viewModel::dismissError,
    )
}
