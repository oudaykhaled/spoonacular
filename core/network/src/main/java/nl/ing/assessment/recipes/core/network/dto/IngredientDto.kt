package nl.ing.assessment.recipes.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class IngredientDto(
    val id: Int? = null,
    val name: String = "",
    val original: String = "",
    val amount: Double = 0.0,
    val unit: String = "",
    val image: String? = null
)
