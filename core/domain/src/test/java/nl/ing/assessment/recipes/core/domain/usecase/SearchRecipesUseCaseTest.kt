package nl.ing.assessment.recipes.core.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import nl.ing.assessment.recipes.core.domain.model.PageResult
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRecipesUseCaseTest {

    private val repository: RecipesRepository = mockk()
    private val useCase = SearchRecipesUseCase(repository)

    @Test
    fun `invoke delegates to repository and returns page result`() = runTest {
        val expected = PageResult(items = emptyList(), totalResults = 0, offset = 0, hasMore = false)
        coEvery { repository.searchRecipes("pasta", SortOrder.POPULARITY, 0, 20) } returns expected

        val result = useCase("pasta", SortOrder.POPULARITY, offset = 0)

        assertEquals(expected, result)
        coVerify(exactly = 1) {
            repository.searchRecipes("pasta", SortOrder.POPULARITY, 0, 20)
        }
    }

    @Test
    fun `invoke forwards custom page size`() = runTest {
        val expected = PageResult(items = emptyList(), totalResults = 100, offset = 40, hasMore = true)
        coEvery { repository.searchRecipes("soup", SortOrder.RELEVANCE, 40, 5) } returns expected

        val result = useCase("soup", SortOrder.RELEVANCE, offset = 40, number = 5)

        assertEquals(expected, result)
        coVerify(exactly = 1) {
            repository.searchRecipes("soup", SortOrder.RELEVANCE, 40, 5)
        }
    }

    @Test
    fun `default page size is 20`() {
        assertEquals(20, SearchRecipesUseCase.DEFAULT_PAGE_SIZE)
    }
}
