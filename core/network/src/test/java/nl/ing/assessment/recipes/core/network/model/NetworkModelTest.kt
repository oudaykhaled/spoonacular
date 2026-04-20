package nl.ing.assessment.recipes.core.network.model

import kotlinx.serialization.json.Json
import nl.ing.assessment.recipes.core.network.dto.AnalyzedInstructionDto
import nl.ing.assessment.recipes.core.network.dto.IngredientDto
import nl.ing.assessment.recipes.core.network.dto.RecipeDetailsDto
import nl.ing.assessment.recipes.core.network.dto.RecipeStepDto
import nl.ing.assessment.recipes.core.network.dto.RecipeSummaryDto
import nl.ing.assessment.recipes.core.network.dto.SearchResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun `SearchResponseDto round-trip with minimal fixture`() {
        val original = SearchResponseDto(
            results = listOf(RecipeSummaryDto(id = 1, title = "T")),
            offset = 0,
            number = 1,
            totalResults = 1
        )
        val encoded = json.encodeToString(SearchResponseDto.serializer(), original)
        val decoded = json.decodeFromString(SearchResponseDto.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `SearchResponseDto tolerates unknown keys`() {
        val payload = """
            {
              "results": [],
              "offset": 0,
              "number": 0,
              "totalResults": 0,
              "processingTimeMs": 3
            }
        """.trimIndent()
        val decoded = json.decodeFromString(SearchResponseDto.serializer(), payload)
        assertEquals(0, decoded.totalResults)
    }

    @Test
    fun `RecipeDetailsDto round-trip with full fixture`() {
        val original = RecipeDetailsDto(
            id = 1,
            title = "Thing",
            image = "i",
            imageType = "png",
            summary = "s",
            readyInMinutes = 20,
            healthScore = 50.0,
            aggregateLikes = 9,
            pricePerServing = 1.23,
            sourceUrl = "url",
            sourceName = "src",
            servings = 2,
            extendedIngredients = listOf(
                IngredientDto(id = 1, name = "n", original = "o", amount = 1.0, unit = "u")
            ),
            instructions = "do it",
            analyzedInstructions = listOf(
                AnalyzedInstructionDto(
                    name = "",
                    steps = listOf(RecipeStepDto(1, "step1"))
                )
            ),
            cuisines = listOf("Italian"),
            dishTypes = listOf("main"),
            diets = listOf("vegan")
        )
        val encoded = json.encodeToString(RecipeDetailsDto.serializer(), original)
        val decoded = json.decodeFromString(RecipeDetailsDto.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `RecipeDetailsDto deserializes minimal fixture with defaults`() {
        val payload = """{"id":42,"title":"Minimal"}"""
        val decoded = json.decodeFromString(RecipeDetailsDto.serializer(), payload)
        assertEquals(42, decoded.id)
        assertEquals("Minimal", decoded.title)
        assertTrue(decoded.extendedIngredients.isEmpty())
        assertTrue(decoded.analyzedInstructions.isEmpty())
        assertTrue(decoded.cuisines.isEmpty())
    }

    @Test
    fun `IngredientDto round-trips preserving null id`() {
        val dto = IngredientDto(id = null, name = "x", original = "y", amount = 0.5, unit = "u")
        val encoded = json.encodeToString(IngredientDto.serializer(), dto)
        val decoded = json.decodeFromString(IngredientDto.serializer(), encoded)
        assertEquals(dto, decoded)
        assertNotNull(encoded)
    }
}
