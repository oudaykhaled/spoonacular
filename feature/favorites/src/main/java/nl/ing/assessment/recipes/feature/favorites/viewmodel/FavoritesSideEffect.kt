package nl.ing.assessment.recipes.feature.favorites.viewmodel

import nl.ing.assessment.recipes.core.designsystem.util.UiText

sealed interface FavoritesSideEffect {
    data class NavigateToDetails(val recipeId: Int) : FavoritesSideEffect
    data class ShowSnackbar(val message: UiText) : FavoritesSideEffect
}
