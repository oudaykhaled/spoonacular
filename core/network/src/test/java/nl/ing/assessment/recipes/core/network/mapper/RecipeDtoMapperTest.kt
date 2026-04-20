package nl.ing.assessment.recipes.core.network.mapper

import kotlinx.serialization.json.Json
import nl.ing.assessment.recipes.core.network.dto.IngredientDto
import nl.ing.assessment.recipes.core.network.dto.RecipeDetailsDto
import nl.ing.assessment.recipes.core.network.dto.RecipeSummaryDto
import nl.ing.assessment.recipes.core.network.dto.SearchResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeDtoMapperTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun `RecipeSummaryDto maps to Recipe with cachedAt`() {
        val dto = RecipeSummaryDto(
            id = 1,
            title = "Pasta",
            image = "img.png",
            readyInMinutes = 30,
            healthScore = 55.5,
            aggregateLikes = 100,
            pricePerServing = 123.4,
            sourceUrl = "http://example.com",
            servings = 4
        )

        val recipe = dto.toDomain(cachedAt = 42L)

        assertEquals(1, recipe.id)
        assertEquals("Pasta", recipe.title)
        assertEquals("img.png", recipe.image)
        assertEquals(30, recipe.readyInMinutes)
        assertEquals(55.5, recipe.healthScore!!, 0.0001)
        assertEquals(42L, recipe.cachedAt)
        assertFalse(recipe.isFavorite)
    }

    @Test
    fun `IngredientDto maps to Ingredient with id defaulting to 0 when null`() {
        val dto = IngredientDto(id = null, name = "salt", original = "1 tsp salt", amount = 1.0, unit = "tsp")
        val ingredient = dto.toDomain()
        assertEquals(0, ingredient.id)
        assertEquals("salt", ingredient.name)
        assertEquals("1 tsp salt", ingredient.original)
    }

    @Test
    fun `IngredientDto maps to Ingredient with provided id`() {
        val dto = IngredientDto(id = 42, name = "flour", original = "2 cups flour", amount = 2.0, unit = "cups")
        val ingredient = dto.toDomain()
        assertEquals(42, ingredient.id)
    }

    @Test
    fun `SearchResponseDto toDomain computes hasMore correctly when more pages available`() {
        val sample = SearchResponseDto(
            results = listOf(
                RecipeSummaryDto(id = 1, title = "A"),
                RecipeSummaryDto(id = 2, title = "B")
            ),
            offset = 0,
            number = 2,
            totalResults = 10
        )

        val page = sample.toDomain(cachedAt = 1L)

        assertEquals(2, page.items.size)
        assertEquals(10, page.totalResults)
        assertEquals(0, page.offset)
        assertTrue(page.hasMore)
    }

    @Test
    fun `SearchResponseDto toDomain computes hasMore false when final page`() {
        val sample = SearchResponseDto(
            results = listOf(RecipeSummaryDto(id = 1, title = "A")),
            offset = 9,
            number = 1,
            totalResults = 10
        )

        val page = sample.toDomain(cachedAt = 1L)

        assertFalse(page.hasMore)
    }

    @Test
    fun `RecipeDetailsDto toDomain flattens analyzed instructions steps`() {
        val payload = """
            {
              "id": 100,
              "title": "Tomato Soup",
              "image": "tomato.png",
              "readyInMinutes": 20,
              "servings": 2,
              "sourceName": "Example",
              "extendedIngredients": [
                {"id": 1, "name": "tomato", "original": "2 tomatoes", "amount": 2.0, "unit": "pc"},
                {"name": "salt", "original": "a pinch", "amount": 0.5, "unit": "tsp"}
              ],
              "instructions": "Mix and cook.",
              "analyzedInstructions": [
                {
                  "name": "",
                  "steps": [
                    {"number": 1, "step": "Chop tomatoes"},
                    {"number": 2, "step": "Boil"}
                  ]
                },
                {
                  "name": "Serving",
                  "steps": [
                    {"number": 1, "step": "Serve hot"}
                  ]
                }
              ],
              "cuisines": ["Italian"],
              "dishTypes": ["soup"],
              "diets": []
            }
        """.trimIndent()

        val dto = json.decodeFromString<RecipeDetailsDto>(payload)
        val details = dto.toDomain(cachedAt = 77L)

        assertEquals(100, details.recipe.id)
        assertEquals("Tomato Soup", details.recipe.title)
        assertEquals(77L, details.recipe.cachedAt)
        assertEquals(2, details.extendedIngredients.size)
        assertEquals(0, details.extendedIngredients[1].id)
        assertEquals(3, details.analyzedInstructionSteps.size)
        assertEquals("Chop tomatoes", details.analyzedInstructionSteps[0].step)
        assertEquals("Serve hot", details.analyzedInstructionSteps[2].step)
        assertEquals(listOf("Italian"), details.cuisines)
        assertEquals(2, details.servings)
        assertEquals("Example", details.sourceName)
    }

    @Test
    fun `SearchResponseDto deserializes sample Spoonacular payload`() {
        val payload = """
            {
              "results": [
                {
                  "id": 716429,
                  "title": "Pasta with Garlic",
                  "image": "https://example.com/img.jpg",
                  "imageType": "jpg",
                  "readyInMinutes": 45,
                  "servings": 2,
                  "healthScore": 19.0
                }
              ],
              "offset": 0,
              "number": 1,
              "totalResults": 1
            }
        """.trimIndent()

        val dto = json.decodeFromString<SearchResponseDto>(payload)
        val page = dto.toDomain(cachedAt = 10L)

        assertEquals(1, page.items.size)
        val first = page.items.first()
        assertEquals(716429, first.id)
        assertEquals("Pasta with Garlic", first.title)
        assertEquals(45, first.readyInMinutes)
        assertNull(first.pricePerServing)
        assertFalse(page.hasMore)
    }
}
