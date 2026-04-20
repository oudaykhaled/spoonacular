package nl.ing.assessment.recipes.feature.search.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.SortOrder

@Immutable
data class SearchUiState(
    val searchInput: String = "",
    val searchQuery: String = "",
    val sort: SortOrder = SortOrder.RELEVANCE,
    val recipes: ImmutableList<Recipe> = persistentListOf(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,
    val offset: Int = 0,
    val hasMorePages: Boolean = true,
    val totalResults: Int = 0,
    val favoriteLoadingIds: ImmutableList<Int> = persistentListOf(),
)
