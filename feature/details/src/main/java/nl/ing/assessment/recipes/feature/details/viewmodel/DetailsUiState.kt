package nl.ing.assessment.recipes.feature.details.viewmodel

import androidx.compose.runtime.Immutable
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails

@Immutable
data class DetailsUiState(
    val details: RecipeDetails? = null,
    val isLoading: Boolean = false,
    val isFavoriteLoading: Boolean = false,
    val error: UiText? = null,
)
