package nl.ing.assessment.recipes.feature.favorites.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.domain.model.Recipe

@Immutable
data class FavoritesUiState(
    val favorites: ImmutableList<Recipe> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val toggleLoadingIds: ImmutableList<Int> = persistentListOf(),
)
