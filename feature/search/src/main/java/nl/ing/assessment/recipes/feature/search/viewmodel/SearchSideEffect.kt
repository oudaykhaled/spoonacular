package nl.ing.assessment.recipes.feature.search.viewmodel

import nl.ing.assessment.recipes.core.designsystem.util.UiText

sealed interface SearchSideEffect {
    data class NavigateToDetails(val recipeId: Int) : SearchSideEffect
    data class ShowSnackbar(val message: UiText) : SearchSideEffect
}
