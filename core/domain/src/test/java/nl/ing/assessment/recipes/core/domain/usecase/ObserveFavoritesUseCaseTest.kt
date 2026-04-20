package nl.ing.assessment.recipes.core.domain.usecase

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveFavoritesUseCaseTest {

    private val repository: RecipesRepository = mockk()
    private val useCase = ObserveFavoritesUseCase(repository)

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val favorites = listOf(
            Recipe(id = 1, title = "A", isFavorite = true, cachedAt = 0L),
            Recipe(id = 2, title = "B", isFavorite = true, cachedAt = 0L)
        )
        every { repository.observeFavorites() } returns flowOf(favorites)

        useCase().test {
            assertEquals(favorites, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) { repository.observeFavorites() }
    }
}
