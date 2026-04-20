package nl.ing.assessment.recipes.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val id: Int,
    val name: String,
    val original: String,
    val amount: Double,
    val unit: String,
    val image: String? = null
)
