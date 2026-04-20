package nl.ing.assessment.recipes.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RecipeStep(
    val number: Int,
    val step: String
)
