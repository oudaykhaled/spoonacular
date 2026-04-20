package nl.ing.assessment.recipes.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecipeStepDto(
    val number: Int = 0,
    val step: String = ""
)
