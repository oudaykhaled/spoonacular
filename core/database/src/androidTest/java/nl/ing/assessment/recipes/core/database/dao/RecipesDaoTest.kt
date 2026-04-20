package nl.ing.assessment.recipes.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import nl.ing.assessment.recipes.core.database.RecipesDatabase
import nl.ing.assessment.recipes.core.database.entity.IngredientEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeStepEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipesDaoTest {

    private lateinit var db: RecipesDatabase
    private lateinit var dao: RecipesDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RecipesDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.recipesDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun recipe(id: Int, cachedAt: Long, isFavorite: Boolean = false) = RecipeEntity(
        id = id,
        title = "Recipe $id",
        cuisinesJson = "[]",
        dishTypesJson = "[]",
        dietsJson = "[]",
        isFavorite = isFavorite,
        cachedAt = cachedAt
    )

    @Test
    fun observeAllRecipesOrdersByCachedAtDesc() = runTest {
        dao.upsertRecipes(
            listOf(
                recipe(id = 1, cachedAt = 100L),
                recipe(id = 2, cachedAt = 200L),
                recipe(id = 3, cachedAt = 150L)
            )
        )

        val list = dao.observeAllRecipes().first()
        assertEquals(listOf(2, 3, 1), list.map { it.id })
    }

    @Test
    fun setFavoriteTogglesAndObserveFavoritesEmits() = runTest {
        dao.upsertRecipes(listOf(recipe(id = 1, cachedAt = 100L)))

        dao.observeFavorites().test {
            assertEquals(emptyList<RecipeEntity>(), awaitItem())
            dao.setFavorite(id = 1, isFav = true)
            val favorites = awaitItem()
            assertEquals(listOf(1), favorites.map { it.id })
            assertTrue(favorites.first().isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertSearchResultsPreservesExistingFavoriteFlag() = runTest {
        dao.upsertRecipes(listOf(recipe(id = 1, cachedAt = 100L, isFavorite = true)))

        dao.upsertSearchResults(
            listOf(
                recipe(id = 1, cachedAt = 500L, isFavorite = false),
                recipe(id = 2, cachedAt = 500L, isFavorite = false)
            )
        )

        val stored = dao.observeAllRecipes().first().associateBy { it.id }
        assertTrue(stored.getValue(1).isFavorite)
        assertEquals(500L, stored.getValue(1).cachedAt)
        assertFalse(stored.getValue(2).isFavorite)
    }

    @Test
    fun upsertRecipeDetailsInsertsIngredientsAndStepsAndObservesRelations() = runTest {
        val recipe = recipe(id = 10, cachedAt = 100L)
        val ingredients = listOf(
            IngredientEntity(
                rowId = 0,
                recipeId = 10,
                externalId = 1,
                name = "salt",
                original = "1 tsp salt",
                amount = 1.0,
                unit = "tsp"
            )
        )
        val steps = listOf(
            RecipeStepEntity(rowId = 0, recipeId = 10, number = 1, step = "boil"),
            RecipeStepEntity(rowId = 0, recipeId = 10, number = 2, step = "stir")
        )

        dao.upsertRecipeDetails(recipe, ingredients, steps)

        dao.observeRecipeWithRelations(10).test {
            val relations = awaitItem()
            assertEquals(10, relations!!.recipe.id)
            assertEquals(listOf(1), relations.ingredients.map { it.externalId })
            assertEquals(listOf(1, 2), relations.steps.map { it.number })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertRecipeDetailsPreservesFavoriteFromExistingRow() = runTest {
        dao.upsertRecipes(listOf(recipe(id = 20, cachedAt = 1L, isFavorite = true)))

        dao.upsertRecipeDetails(
            recipe = recipe(id = 20, cachedAt = 2L, isFavorite = false),
            ingredients = emptyList(),
            steps = emptyList()
        )

        val stored = dao.getRecipe(20)
        assertTrue(stored!!.isFavorite)
    }
}
