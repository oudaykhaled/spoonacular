package nl.ing.assessment.recipes.core.domain.usecase

import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import javax.inject.Inject

class GetRecipeDetailsUseCase @Inject constructor(
    private val repository: RecipesRepository
) {
    suspend operator fun invoke(recipeId: Int): RecipeDetails =
        repository.fetchRecipeDetails(recipeId)
}
