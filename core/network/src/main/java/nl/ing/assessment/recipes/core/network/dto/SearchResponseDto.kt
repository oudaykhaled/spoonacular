package nl.ing.assessment.recipes.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val results: List<RecipeSummaryDto> = emptyList(),
    val offset: Int = 0,
    val number: Int = 0,
    val totalResults: Int = 0
)
