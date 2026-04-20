package nl.ing.assessment.recipes.feature.details.viewmodel

import app.cash.turbine.test
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.ing.assessment.recipes.core.domain.usecase.GetRecipeDetailsUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ObserveRecipeDetailsUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ToggleFavoriteUseCase
import nl.ing.assessment.recipes.core.telemetry.EventTracker
import nl.ing.assessment.recipes.core.testing.FakeRecipesRepository
import nl.ing.assessment.recipes.core.testing.TestFixtures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeRecipesRepository
    private lateinit var eventTracker: EventTracker

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRecipesRepository()
        eventTracker = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(recipeId: Int = RECIPE_ID) = DetailsViewModel(
        recipeId = recipeId,
        getRecipeDetails = GetRecipeDetailsUseCase(repository),
        observeRecipeDetails = ObserveRecipeDetailsUseCase(repository),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(repository),
        eventTracker = eventTracker,
    )

    @Test
    fun `init tracks screen view and fetch populates state`() = runTest(dispatcher) {
        val details = TestFixtures.recipeDetails(id = RECIPE_ID)
        repository.setDetails(RECIPE_ID, details)

        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        verify { eventTracker.trackScreenView("DetailsScreen") }
        val state = vm.state.value
        assertEquals(details, state.details)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `fetch failure sets error on state`() = runTest(dispatcher) {
        repository.failDetails = true

        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `onOpenSource emits OpenUrl side effect with source url`() = runTest(dispatcher) {
        val recipe = TestFixtures.recipe(id = RECIPE_ID, sourceUrl = SOURCE_URL)
        val details = TestFixtures.recipeDetails(id = RECIPE_ID, recipe = recipe)
        repository.setDetails(RECIPE_ID, details)

        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.sideEffects.test {
            vm.onOpenSource()
            dispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is DetailsSideEffect.OpenUrl)
            assertEquals(SOURCE_URL, (effect as DetailsSideEffect.OpenUrl).url)
        }
    }

    @Test
    fun `toggleFavorite calls repo and flips favorite state`() = runTest(dispatcher) {
        val recipe = TestFixtures.recipe(id = RECIPE_ID, isFavorite = false)
        val details = TestFixtures.recipeDetails(id = RECIPE_ID, recipe = recipe)
        repository.emitCached(listOf(recipe))
        repository.setDetails(RECIPE_ID, details)

        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggleFavorite()
        dispatcher.scheduler.advanceUntilIdle()

        val favorites = repository.observeFavorites().first()
        assertTrue(favorites.any { it.id == RECIPE_ID })
    }

    private companion object {
        const val RECIPE_ID = 42
        const val SOURCE_URL = "https://example.com/recipe/42"
    }
}
