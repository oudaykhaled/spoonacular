package nl.ing.assessment.recipes.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import javax.inject.Inject

class ObserveRecipeDetailsUseCase @Inject constructor(
    private val repository: RecipesRepository
) {
    operator fun invoke(recipeId: Int): Flow<RecipeDetails?> =
        repository.observeRecipeDetails(recipeId)
}
