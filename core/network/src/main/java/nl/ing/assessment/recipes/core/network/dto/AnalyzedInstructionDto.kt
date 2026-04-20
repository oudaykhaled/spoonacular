package nl.ing.assessment.recipes.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedInstructionDto(
    val name: String = "",
    val steps: List<RecipeStepDto> = emptyList()
)
