package nl.ing.assessment.recipes.core.domain.model

data class PageResult(
    val items: List<Recipe>,
    val totalResults: Int,
    val offset: Int,
    val hasMore: Boolean
)
