package nl.ing.assessment.recipes.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import javax.inject.Inject

class ObserveCachedRecipesUseCase @Inject constructor(
    private val repository: RecipesRepository
) {
    operator fun invoke(): Flow<List<Recipe>> = repository.observeCachedRecipes()
}
