package nl.ing.assessment.recipes.core.domain.model

import androidx.compose.runtime.Stable

@Stable
enum class SortOrder {
    RELEVANCE,
    POPULARITY,
    HEALTHINESS,
    TIME,
    PRICE
}

fun SortOrder.toApiParam(): String? = when (this) {
    SortOrder.RELEVANCE -> null
    SortOrder.POPULARITY -> "popularity"
    SortOrder.HEALTHINESS -> "healthiness"
    SortOrder.TIME -> "time"
    SortOrder.PRICE -> "price"
}
