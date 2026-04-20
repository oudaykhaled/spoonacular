package nl.ing.assessment.recipes.core.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import nl.ing.assessment.recipes.core.database.dao.RecipesDao
import nl.ing.assessment.recipes.core.database.mapper.toDomain
import nl.ing.assessment.recipes.core.database.mapper.toDomainDetails
import nl.ing.assessment.recipes.core.database.mapper.toEntity
import nl.ing.assessment.recipes.core.domain.model.PageResult
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.core.domain.model.ServerException
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.core.domain.model.toApiParam
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import nl.ing.assessment.recipes.core.logging.Logger
import nl.ing.assessment.recipes.core.network.api.SpoonacularApi
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipesRepositoryImpl @Inject constructor(
    private val dao: RecipesDao,
    private val api: SpoonacularApi,
    private val logger: Logger,
    private val json: Json,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : RecipesRepository {

    override fun observeCachedRecipes(): Flow<List<Recipe>> =
        dao.observeAllRecipes().map { list -> list.map { it.toDomain() } }

    override fun observeFavorites(): Flow<List<Recipe>> =
        dao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override fun observeRecipeDetails(recipeId: Int): Flow<RecipeDetails?> =
        dao.observeRecipeWithRelations(recipeId).map { it?.toDomainDetails(json) }

    override suspend fun getRecipeById(recipeId: Int): Recipe? =
        dao.getRecipe(recipeId)?.toDomain()

    override suspend fun searchRecipes(
        query: String,
        sort: SortOrder,
        offset: Int,
        number: Int
    ): PageResult {
        val start = clock()
        val response = wrapHttpException {
            api.searchRecipes(
                query = query,
                sort = sort.toApiParam(),
                sortDirection = null,
                offset = offset,
                number = number
            )
        }
        logger.logNetworkRequest(
            method = "GET",
            url = "recipes/complexSearch?query=$query&offset=$offset&number=$number",
            durationMs = clock() - start,
            statusCode = 200
        )
        val now = clock()
        val entities = response.results.map { dto ->
            RecipeMapperHelper.summaryToEntity(dto, now, json)
        }
        dao.upsertSearchResults(entities)
        val favorites = dao.getFavoriteIds().toSet()
        val items = entities.map { entity ->
            val withFavorite = if (entity.id in favorites) entity.copy(isFavorite = true) else entity
            withFavorite.toDomain()
        }
        return PageResult(
            items = items,
            totalResults = response.totalResults,
            offset = response.offset,
            hasMore = (response.offset + response.results.size) < response.totalResults
        )
    }

    override suspend fun fetchRecipeDetails(recipeId: Int): RecipeDetails {
        val start = clock()
        val response = wrapHttpException {
            api.getRecipeDetails(id = recipeId)
        }
        logger.logNetworkRequest(
            method = "GET",
            url = "recipes/$recipeId/information",
            durationMs = clock() - start,
            statusCode = 200
        )
        val now = clock()
        val recipeEntity = RecipeMapperHelper.detailsToEntity(response, now, json)
        val ingredientEntities = response.extendedIngredients.map { dto ->
            nl.ing.assessment.recipes.core.database.entity.IngredientEntity(
                rowId = 0,
                recipeId = recipeId,
                externalId = dto.id ?: 0,
                name = dto.name,
                original = dto.original,
                amount = dto.amount,
                unit = dto.unit,
                image = dto.image
            )
        }
        val stepEntities = response.analyzedInstructions
            .flatMap { it.steps }
            .map { step ->
                nl.ing.assessment.recipes.core.database.entity.RecipeStepEntity(
                    rowId = 0,
                    recipeId = recipeId,
                    number = step.number,
                    step = step.step
                )
            }
        dao.upsertRecipeDetails(recipeEntity, ingredientEntities, stepEntities)
        val stored = dao.getRecipeWithRelations(recipeId)
            ?: error("Failed to store recipe details for id=$recipeId")
        return stored.toDomainDetails(json)
    }

    override suspend fun toggleFavorite(recipeId: Int, isCurrentlyFavorite: Boolean) {
        dao.setFavorite(recipeId, !isCurrentlyFavorite)
    }

    private suspend fun <T> wrapHttpException(block: suspend () -> T): T =
        try {
            block()
        } catch (e: HttpException) {
            logger.logError(
                tag = "RecipesRepository",
                error = e,
                context = mapOf("httpCode" to e.code())
            )
            throw ServerException(code = e.code(), message = e.message())
        }
}
