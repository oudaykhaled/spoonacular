package nl.ing.assessment.recipes.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecipeSummaryDto(
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
    val servings: Int? = null
)
