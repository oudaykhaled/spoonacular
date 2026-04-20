package nl.ing.assessment.recipes.feature.details.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import nl.ing.assessment.recipes.core.designsystem.component.ErrorState
import nl.ing.assessment.recipes.core.designsystem.component.FavoriteButton
import nl.ing.assessment.recipes.core.designsystem.component.LoadingState
import nl.ing.assessment.recipes.core.designsystem.theme.spacing
import nl.ing.assessment.recipes.core.designsystem.preview.LightDarkPreview
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.feature.details.R
import nl.ing.assessment.recipes.feature.details.presentation.components.DetailsHeader
import nl.ing.assessment.recipes.feature.details.presentation.components.IngredientsSection
import nl.ing.assessment.recipes.feature.details.presentation.components.InstructionsSection
import nl.ing.assessment.recipes.feature.details.presentation.components.SourceButton
import nl.ing.assessment.recipes.feature.details.viewmodel.DetailsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    state: DetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSource: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("details_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.details?.recipe?.title
                            ?: stringResource(R.string.details_title_fallback),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.details_back),
                        )
                    }
                },
                actions = {
                    if (state.details != null) {
                        FavoriteButton(
                            isFavorite = state.details.recipe.isFavorite,
                            onClick = onToggleFavorite,
                        )
                    }
                },
            )
        },
    ) { padding ->
        DetailsBody(
            state = state,
            onRetry = onRetry,
            onOpenSource = onOpenSource,
            onDismissError = onDismissError,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
private fun DetailsBody(
    state: DetailsUiState,
    onRetry: () -> Unit,
    onOpenSource: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = state.details
    when {
        state.isLoading && details == null -> LoadingState(modifier = modifier)
        state.error != null && details == null -> ErrorState(
            message = state.error,
            onRetry = {
                onDismissError()
                onRetry()
            },
            modifier = modifier,
        )
        details != null -> DetailsContent(
            state = state,
            onOpenSource = onOpenSource,
            modifier = modifier,
        )
    }
}

@Composable
private fun DetailsContent(
    state: DetailsUiState,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = state.details ?: return
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.extraLarge)
            .fillMaxWidth(),
    ) {
        DetailsHeader(details = details)

        Spacer(Modifier.height(MaterialTheme.spacing.xxxLarge))

        IngredientsSection(ingredients = details.extendedIngredients)

        Spacer(Modifier.height(MaterialTheme.spacing.xxxLarge))

        InstructionsSection(
            steps = details.analyzedInstructionSteps,
            fallbackInstructions = details.instructions,
        )

        val sourceUrl = details.recipe.sourceUrl
        if (!sourceUrl.isNullOrBlank()) {
            Spacer(Modifier.height(MaterialTheme.spacing.xxxLarge))
            SourceButton(onClick = onOpenSource)
        }

        Spacer(Modifier.height(MaterialTheme.spacing.huge))
    }
}

@LightDarkPreview
@Suppress("UnusedPrivateMember") // Preview is used by Android Studio; not a runtime caller
@Composable
private fun DetailsScreenPreview() {
    RecipesTheme {
        DetailsScreen(
            state = DetailsUiState(),
            onBack = {},
            onRetry = {},
            onToggleFavorite = {},
            onOpenSource = {},
            onDismissError = {},
        )
    }
}
