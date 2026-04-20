package nl.ing.assessment.recipes.core.domain.usecase

import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: RecipesRepository
) {
    suspend operator fun invoke(recipeId: Int, isCurrentlyFavorite: Boolean) {
        repository.toggleFavorite(recipeId, isCurrentlyFavorite)
    }
}
