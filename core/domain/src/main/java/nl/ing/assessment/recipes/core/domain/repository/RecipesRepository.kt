package nl.ing.assessment.recipes.core.domain.repository

import kotlinx.coroutines.flow.Flow
import nl.ing.assessment.recipes.core.domain.model.PageResult
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.core.domain.model.SortOrder

interface RecipesRepository {
    fun observeCachedRecipes(): Flow<List<Recipe>>
    fun observeFavorites(): Flow<List<Recipe>>
    fun observeRecipeDetails(recipeId: Int): Flow<RecipeDetails?>
    suspend fun getRecipeById(recipeId: Int): Recipe?
    suspend fun searchRecipes(query: String, sort: SortOrder, offset: Int, number: Int): PageResult
    suspend fun fetchRecipeDetails(recipeId: Int): RecipeDetails
    suspend fun toggleFavorite(recipeId: Int, isCurrentlyFavorite: Boolean)
}
