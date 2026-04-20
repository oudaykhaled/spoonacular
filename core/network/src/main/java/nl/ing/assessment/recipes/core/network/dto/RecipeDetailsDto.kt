package nl.ing.assessment.recipes.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecipeDetailsDto(
    val id: Int,
    val title: String,
    val image: String? = null,
    val imageType: String? = null,
    val summary: String? = null,
    val readyInMinutes: Int? = null,
    val healthScore: Double? = null,
    val aggregateLikes: Int? = null,
    val pricePerServing: Double? = null,
    val sourceUrl: String? = null,
    val sourceName: String? = null,
    val servings: Int? = null,
    val extendedIngredients: List<IngredientDto> = emptyList(),
    val instructions: String? = null,
    val analyzedInstructions: List<AnalyzedInstructionDto> = emptyList(),
    val cuisines: List<String> = emptyList(),
    val dishTypes: List<String> = emptyList(),
    val diets: List<String> = emptyList()
)
