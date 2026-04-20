package nl.ing.assessment.recipes.core.domain.usecase

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private val repository: RecipesRepository = mockk()
    private val useCase = ToggleFavoriteUseCase(repository)

    @Test
    fun `invoke delegates to repository with currently favorite flag`() = runTest {
        coJustRun { repository.toggleFavorite(42, true) }

        useCase(recipeId = 42, isCurrentlyFavorite = true)

        coVerify(exactly = 1) { repository.toggleFavorite(42, true) }
    }

    @Test
    fun `invoke delegates when not currently favorite`() = runTest {
        coJustRun { repository.toggleFavorite(7, false) }

        useCase(recipeId = 7, isCurrentlyFavorite = false)

        coVerify(exactly = 1) { repository.toggleFavorite(7, false) }
    }
}
