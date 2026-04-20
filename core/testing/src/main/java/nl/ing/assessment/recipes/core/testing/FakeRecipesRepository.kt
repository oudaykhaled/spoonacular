package nl.ing.assessment.recipes.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import nl.ing.assessment.recipes.core.domain.model.PageResult
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import java.io.IOException

class FakeRecipesRepository : RecipesRepository {

    private val cachedFlow = MutableStateFlow<List<Recipe>>(emptyList())
    private val favoritesFlow = MutableStateFlow<List<Recipe>>(emptyList())
    private val detailsFlow = MutableStateFlow<Map<Int, RecipeDetails>>(emptyMap())

    var failSearch: Boolean = false
    var failDetails: Boolean = false
    var failFavorite: Boolean = false
    var fakeTotalResults: Int = 0
    var fakeHasMore: Boolean = false

    fun emitCached(list: List<Recipe>) {
        cachedFlow.value = list
    }

    fun emitFavorites(list: List<Recipe>) {
        favoritesFlow.value = list
    }

    fun setDetails(id: Int, details: RecipeDetails) {
        detailsFlow.value = detailsFlow.value + (id to details)
    }

    fun clearDetails() {
        detailsFlow.value = emptyMap()
    }

    override fun observeCachedRecipes(): Flow<List<Recipe>> = cachedFlow

    override fun observeFavorites(): Flow<List<Recipe>> = favoritesFlow

    override fun observeRecipeDetails(recipeId: Int): Flow<RecipeDetails?> =
        detailsFlow.map { it[recipeId] }

    override suspend fun getRecipeById(recipeId: Int): Recipe? =
        cachedFlow.value.find { it.id == recipeId }

    override suspend fun searchRecipes(
        query: String,
        sort: SortOrder,
        offset: Int,
        number: Int
    ): PageResult {
        if (failSearch) throw IOException("fail")
        val items = cachedFlow.value.drop(offset).take(number)
        return PageResult(
            items = items,
            totalResults = if (fakeTotalResults != 0) fakeTotalResults else cachedFlow.value.size,
            offset = offset,
            hasMore = fakeHasMore || (offset + items.size) < cachedFlow.value.size
        )
    }

    override suspend fun fetchRecipeDetails(recipeId: Int): RecipeDetails {
        if (failDetails) throw IOException("fail")
        val existing = detailsFlow.value[recipeId]
        if (existing != null) return existing
        val details = TestFixtures.recipeDetails(id = recipeId)
        detailsFlow.value = detailsFlow.value + (recipeId to details)
        return details
    }

    override suspend fun toggleFavorite(recipeId: Int, isCurrentlyFavorite: Boolean) {
        if (failFavorite) throw IOException("fail")
        cachedFlow.value = cachedFlow.value.map { recipe ->
            if (recipe.id == recipeId) recipe.copy(isFavorite = !isCurrentlyFavorite) else recipe
        }
        val newFavorite = !isCurrentlyFavorite
        favoritesFlow.value = if (newFavorite) {
            val toAdd = cachedFlow.value.find { it.id == recipeId }
            if (toAdd != null && favoritesFlow.value.none { it.id == recipeId }) {
                favoritesFlow.value + toAdd
            } else {
                favoritesFlow.value
            }
        } else {
            favoritesFlow.value.filterNot { it.id == recipeId }
        }
    }
}
