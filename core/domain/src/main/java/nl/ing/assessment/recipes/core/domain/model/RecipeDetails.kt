package nl.ing.assessment.recipes.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RecipeDetails(
    val recipe: Recipe,
    val extendedIngredients: List<Ingredient> = emptyList(),
    val instructions: String? = null,
    val analyzedInstructionSteps: List<RecipeStep> = emptyList(),
    val sourceName: String? = null,
    val servings: Int? = null,
    val cuisines: List<String> = emptyList(),
    val dishTypes: List<String> = emptyList(),
    val diets: List<String> = emptyList()
)
