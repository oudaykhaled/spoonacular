package nl.ing.assessment.recipes.core.database.repository

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import nl.ing.assessment.recipes.core.database.dao.RecipesDao
import nl.ing.assessment.recipes.core.database.entity.IngredientEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeStepEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeWithRelations
import nl.ing.assessment.recipes.core.domain.model.ServerException
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.core.logging.Logger
import nl.ing.assessment.recipes.core.network.api.SpoonacularApi
import nl.ing.assessment.recipes.core.network.dto.AnalyzedInstructionDto
import nl.ing.assessment.recipes.core.network.dto.IngredientDto
import nl.ing.assessment.recipes.core.network.dto.RecipeDetailsDto
import nl.ing.assessment.recipes.core.network.dto.RecipeStepDto
import nl.ing.assessment.recipes.core.network.dto.RecipeSummaryDto
import nl.ing.assessment.recipes.core.network.dto.SearchResponseDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RecipesRepositoryImplTest {

    private val dao: RecipesDao = mockk(relaxUnitFun = true)
    private val api: SpoonacularApi = mockk()
    private val logger: Logger = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val clock: () -> Long = { 1_000L }

    private val repository = RecipesRepositoryImpl(
        dao = dao,
        api = api,
        logger = logger,
        json = json,
        clock = clock
    )

    @Test
    fun `searchRecipes upserts mapped entities and preserves favorites`() = runTest {
        val dto = SearchResponseDto(
            results = listOf(
                RecipeSummaryDto(id = 1, title = "A"),
                RecipeSummaryDto(id = 2, title = "B")
            ),
            offset = 0,
            number = 2,
            totalResults = 10
        )
        coEvery {
            api.searchRecipes(
                query = "pasta",
                sort = null,
                sortDirection = null,
                offset = 0,
                number = 2
            )
        } returns dto
        coEvery { dao.upsertSearchResults(any()) } just Runs
        coEvery { dao.getFavoriteIds() } returns listOf(2)

        val page = repository.searchRecipes(
            query = "pasta",
            sort = SortOrder.RELEVANCE,
            offset = 0,
            number = 2
        )

        assertEquals(2, page.items.size)
        assertEquals(10, page.totalResults)
        assertEquals(0, page.offset)
        assertTrue(page.hasMore)
        assertTrue(page.items.first { it.id == 2 }.isFavorite)
        assertFalse(page.items.first { it.id == 1 }.isFavorite)

        val captured = slot<List<RecipeEntity>>()
        coVerify(exactly = 1) { dao.upsertSearchResults(capture(captured)) }
        assertEquals(listOf(1, 2), captured.captured.map { it.id })
    }

    @Test
    fun `searchRecipes maps 402 HttpException to ServerException with code 402`() = runTest {
        val httpException = HttpException(
            Response.error<Any>(
                402,
                "quota".toResponseBody("text/plain".toMediaTypeOrNull())
            )
        )
        coEvery {
            api.searchRecipes(any(), any(), any(), any(), any())
        } throws httpException

        val error = runCatching {
            repository.searchRecipes(
                query = "q",
                sort = SortOrder.POPULARITY,
                offset = 0,
                number = 10
            )
        }.exceptionOrNull()

        assertTrue(error is ServerException)
        assertEquals(402, (error as ServerException).code)
    }

    @Test
    fun `fetchRecipeDetails maps 429 HttpException to ServerException`() = runTest {
        val httpException = HttpException(
            Response.error<Any>(
                429,
                "rate".toResponseBody("text/plain".toMediaTypeOrNull())
            )
        )
        coEvery { api.getRecipeDetails(any(), any()) } throws httpException

        val error = runCatching { repository.fetchRecipeDetails(1) }.exceptionOrNull()

        assertTrue(error is ServerException)
        assertEquals(429, (error as ServerException).code)
    }

    @Suppress("LongMethod")
    @Test
    fun `fetchRecipeDetails persists recipe ingredients and steps`() = runTest {
        val dto = RecipeDetailsDto(
            id = 11,
            title = "Pad Thai",
            extendedIngredients = listOf(
                IngredientDto(id = 100, name = "noodles", original = "1 pack", amount = 1.0, unit = "pack")
            ),
            analyzedInstructions = listOf(
                AnalyzedInstructionDto(
                    name = "main",
                    steps = listOf(
                        RecipeStepDto(number = 1, step = "boil"),
                        RecipeStepDto(number = 2, step = "stir")
                    )
                )
            ),
            cuisines = listOf("Thai"),
            dishTypes = listOf("main"),
            diets = emptyList(),
            instructions = "boil and stir"
        )
        coEvery { api.getRecipeDetails(11, false) } returns dto
        coEvery { dao.upsertRecipeDetails(any(), any(), any()) } just Runs

        val storedEntity = RecipeEntity(
            id = 11,
            title = "Pad Thai",
            cuisinesJson = """["Thai"]""",
            dishTypesJson = """["main"]""",
            dietsJson = "[]",
            instructions = "boil and stir",
            sourceName = null,
            servings = null,
            cachedAt = 1_000L
        )
        val storedRelations = RecipeWithRelations(
            recipe = storedEntity,
            ingredients = listOf(
                IngredientEntity(
                    rowId = 1,
                    recipeId = 11,
                    externalId = 100,
                    name = "noodles",
                    original = "1 pack",
                    amount = 1.0,
                    unit = "pack"
                )
            ),
            steps = listOf(
                RecipeStepEntity(rowId = 1, recipeId = 11, number = 1, step = "boil"),
                RecipeStepEntity(rowId = 2, recipeId = 11, number = 2, step = "stir")
            )
        )
        coEvery { dao.getRecipeWithRelations(11) } returns storedRelations

        val details = repository.fetchRecipeDetails(11)

        val recipeSlot = slot<RecipeEntity>()
        val ingredientsSlot = slot<List<IngredientEntity>>()
        val stepsSlot = slot<List<RecipeStepEntity>>()
        coVerify(exactly = 1) {
            dao.upsertRecipeDetails(
                capture(recipeSlot),
                capture(ingredientsSlot),
                capture(stepsSlot)
            )
        }
        assertEquals(11, recipeSlot.captured.id)
        assertTrue(recipeSlot.captured.hasDetails)
        assertTrue(recipeSlot.captured.cuisinesJson.contains("Thai"))
        assertEquals(listOf(100), ingredientsSlot.captured.map { it.externalId })
        assertEquals(listOf(1, 2), stepsSlot.captured.map { it.number })

        assertEquals("Pad Thai", details.recipe.title)
        assertEquals(listOf("Thai"), details.cuisines)
        assertEquals(listOf(1, 2), details.analyzedInstructionSteps.map { it.number })
    }

    @Test
    fun `observeFavorites maps entities to domain`() = runTest {
        val entities = listOf(
            RecipeEntity(id = 1, title = "A", isFavorite = true, cachedAt = 0L),
            RecipeEntity(id = 2, title = "B", isFavorite = true, cachedAt = 0L)
        )
        coEvery { dao.observeFavorites() } returns flowOf(entities)

        repository.observeFavorites().test {
            val received = awaitItem()
            assertEquals(listOf(1, 2), received.map { it.id })
            assertTrue(received.all { it.isFavorite })
            awaitComplete()
        }
    }

    @Test
    fun `observeCachedRecipes delegates to dao`() = runTest {
        val entities = listOf(RecipeEntity(id = 1, title = "A", cachedAt = 0L))
        coEvery { dao.observeAllRecipes() } returns flowOf(entities)

        repository.observeCachedRecipes().test {
            assertEquals(listOf(1), awaitItem().map { it.id })
            awaitComplete()
        }
    }

    @Test
    fun `observeRecipeDetails maps when entity is present`() = runTest {
        val relations = RecipeWithRelations(
            recipe = RecipeEntity(
                id = 7,
                title = "R",
                cuisinesJson = "[]",
                dishTypesJson = "[]",
                dietsJson = "[]",
                cachedAt = 0L
            ),
            ingredients = emptyList(),
            steps = emptyList()
        )
        coEvery { dao.observeRecipeWithRelations(7) } returns flowOf(relations)

        repository.observeRecipeDetails(7).test {
            val details = awaitItem()
            assertEquals(7, details!!.recipe.id)
            awaitComplete()
        }
    }

    @Test
    fun `observeRecipeDetails emits null when no entity`() = runTest {
        coEvery { dao.observeRecipeWithRelations(99) } returns flowOf(null)

        repository.observeRecipeDetails(99).test {
            assertEquals(null, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getRecipeById returns mapped domain or null`() = runTest {
        coEvery { dao.getRecipe(1) } returns RecipeEntity(id = 1, title = "T", cachedAt = 0L)
        coEvery { dao.getRecipe(2) } returns null

        assertEquals(1, repository.getRecipeById(1)?.id)
        assertEquals(null, repository.getRecipeById(2))
    }

    @Test
    fun `toggleFavorite flips flag true to false`() = runTest {
        repository.toggleFavorite(recipeId = 5, isCurrentlyFavorite = true)
        coVerify(exactly = 1) { dao.setFavorite(id = 5, isFav = false) }
    }

    @Test
    fun `toggleFavorite flips flag false to true`() = runTest {
        repository.toggleFavorite(recipeId = 6, isCurrentlyFavorite = false)
        coVerify(exactly = 1) { dao.setFavorite(id = 6, isFav = true) }
    }
}
