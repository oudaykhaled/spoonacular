package nl.ing.assessment.recipes.core.database.mapper

import kotlinx.serialization.json.Json
import nl.ing.assessment.recipes.core.database.entity.IngredientEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeStepEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeWithRelations
import nl.ing.assessment.recipes.core.domain.model.Ingredient
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeEntityMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `RecipeEntity toDomain maps all fields`() {
        val entity = RecipeEntity(
            id = 42,
            title = "Pasta",
            image = "img",
            imageType = "jpg",
            summary = "summary",
            readyInMinutes = 30,
            healthScore = 88.5,
            aggregateLikes = 100,
            pricePerServing = 1.25,
            sourceUrl = "url",
            sourceName = "chef",
            servings = 2,
            isFavorite = true,
            hasDetails = true,
            cuisinesJson = "[]",
            dishTypesJson = "[]",
            dietsJson = "[]",
            instructions = "cook it",
            cachedAt = 1000L
        )

        val domain = entity.toDomain()

        assertEquals(42, domain.id)
        assertEquals("Pasta", domain.title)
        assertEquals("img", domain.image)
        assertEquals(30, domain.readyInMinutes)
        assertEquals(88.5, domain.healthScore!!, 0.0)
        assertTrue(domain.isFavorite)
        assertEquals(1000L, domain.cachedAt)
    }

    @Test
    fun `Recipe toEntity preserves fields and encodes list fields`() {
        val recipe = Recipe(
            id = 7,
            title = "Soup",
            image = "img",
            summary = "s",
            readyInMinutes = 10,
            healthScore = 50.0,
            aggregateLikes = 3,
            pricePerServing = 2.0,
            sourceUrl = "u",
            isFavorite = false,
            cachedAt = 123L
        )

        val entity = recipe.toEntity(
            cachedAt = 456L,
            hasDetails = true,
            json = json,
            cuisines = listOf("Italian"),
            dishTypes = listOf("main"),
            diets = listOf("vegan"),
            instructions = "boil",
            sourceName = "source",
            servings = 4
        )

        assertEquals(7, entity.id)
        assertEquals("Soup", entity.title)
        assertEquals(456L, entity.cachedAt)
        assertTrue(entity.hasDetails)
        assertTrue(entity.cuisinesJson.contains("Italian"))
        assertTrue(entity.dishTypesJson.contains("main"))
        assertTrue(entity.dietsJson.contains("vegan"))
        assertEquals("boil", entity.instructions)
        assertEquals("source", entity.sourceName)
        assertEquals(4, entity.servings)
    }

    @Test
    fun `RecipeWithRelations toDomainDetails parses json lists and maps relations`() {
        val recipe = RecipeEntity(
            id = 1,
            title = "Dish",
            cuisinesJson = """["Thai","Asian"]""",
            dishTypesJson = """["lunch"]""",
            dietsJson = """["gluten free"]""",
            instructions = "steps",
            sourceName = "name",
            servings = 3,
            cachedAt = 0L
        )
        val ingredients = listOf(
            IngredientEntity(
                rowId = 1,
                recipeId = 1,
                externalId = 10,
                name = "salt",
                original = "1 tsp salt",
                amount = 1.0,
                unit = "tsp",
                image = null
            )
        )
        val steps = listOf(
            RecipeStepEntity(rowId = 2, recipeId = 1, number = 2, step = "second"),
            RecipeStepEntity(rowId = 1, recipeId = 1, number = 1, step = "first")
        )

        val details = RecipeWithRelations(recipe, ingredients, steps).toDomainDetails(json)

        assertEquals(listOf("Thai", "Asian"), details.cuisines)
        assertEquals(listOf("lunch"), details.dishTypes)
        assertEquals(listOf("gluten free"), details.diets)
        assertEquals("steps", details.instructions)
        assertEquals(1, details.extendedIngredients.size)
        assertEquals(10, details.extendedIngredients.first().id)
        assertEquals(listOf(1, 2), details.analyzedInstructionSteps.map { it.number })
    }

    @Test
    fun `RecipeWithRelations toDomainDetails tolerates malformed json fields`() {
        val recipe = RecipeEntity(
            id = 1,
            title = "Dish",
            cuisinesJson = "not-json",
            dishTypesJson = "not-json",
            dietsJson = "not-json",
            cachedAt = 0L
        )

        val details = RecipeWithRelations(recipe, emptyList(), emptyList()).toDomainDetails(json)

        assertEquals(emptyList<String>(), details.cuisines)
        assertEquals(emptyList<String>(), details.dishTypes)
        assertEquals(emptyList<String>(), details.diets)
    }

    @Test
    fun `Ingredient and RecipeStep roundtrip through entity`() {
        val ingredient = Ingredient(
            id = 5,
            name = "sugar",
            original = "2 tbsp sugar",
            amount = 2.0,
            unit = "tbsp",
            image = "img"
        )
        val step = RecipeStep(number = 3, step = "stir")

        val ingredientEntity = ingredient.toEntity(recipeId = 100)
        val stepEntity = step.toEntity(recipeId = 100)

        assertEquals(0L, ingredientEntity.rowId)
        assertEquals(100, ingredientEntity.recipeId)
        assertEquals(5, ingredientEntity.externalId)
        assertEquals("sugar", ingredientEntity.name)

        assertEquals(100, stepEntity.recipeId)
        assertEquals(3, stepEntity.number)
        assertEquals("stir", stepEntity.step)

        val roundTripped = ingredientEntity.toDomain()
        assertEquals(ingredient, roundTripped)
        assertEquals(step, stepEntity.toDomain())
    }

    @Test
    fun `RecipeEntity default isFavorite is false`() {
        val entity = RecipeEntity(id = 1, title = "t", cachedAt = 0L)
        assertNotNull(entity)
        assertFalse(entity.isFavorite)
        assertFalse(entity.hasDetails)
    }
}
