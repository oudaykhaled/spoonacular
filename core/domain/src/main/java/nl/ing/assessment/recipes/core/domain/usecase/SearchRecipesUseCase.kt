package nl.ing.assessment.recipes.core.domain.usecase

import nl.ing.assessment.recipes.core.domain.model.PageResult
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import javax.inject.Inject

class SearchRecipesUseCase @Inject constructor(
    private val repository: RecipesRepository
) {
    suspend operator fun invoke(
        query: String,
        sort: SortOrder,
        offset: Int,
        number: Int = DEFAULT_PAGE_SIZE
    ): PageResult = repository.searchRecipes(query, sort, offset, number)

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}
