package nl.ing.assessment.recipes.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
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
    val isFavorite: Boolean = false,
    val cachedAt: Long
)
