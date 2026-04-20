package nl.ing.assessment.recipes.feature.favorites.viewmodel

import app.cash.turbine.test
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.ing.assessment.recipes.core.domain.usecase.ObserveFavoritesUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ToggleFavoriteUseCase
import nl.ing.assessment.recipes.core.telemetry.EventTracker
import nl.ing.assessment.recipes.core.testing.FakeRecipesRepository
import nl.ing.assessment.recipes.core.testing.TestFixtures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var fake: FakeRecipesRepository
    private lateinit var observeFavorites: ObserveFavoritesUseCase
    private lateinit var toggleFavorite: ToggleFavoriteUseCase
    private lateinit var eventTracker: EventTracker
    private lateinit var viewModel: FavoritesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fake = FakeRecipesRepository()
        observeFavorites = ObserveFavoritesUseCase(fake)
        toggleFavorite = ToggleFavoriteUseCase(fake)
        eventTracker = mockk(relaxed = true)
        viewModel = FavoritesViewModel(observeFavorites, toggleFavorite, eventTracker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits favorites and clears loading when repository emits list`() = runTest(dispatcher) {
        val recipe1 = TestFixtures.recipe(id = 1, title = "One", isFavorite = true)
        val recipe2 = TestFixtures.recipe(id = 2, title = "Two", isFavorite = true)

        viewModel.state.test {
            // Drain any state emissions that happened during init on the test dispatcher.
            skipItems(1)

            fake.emitFavorites(listOf(recipe1, recipe2))

            val updated = awaitItem()
            assertEquals(listOf(recipe1, recipe2), updated.favorites.toList())
            assertEquals(false, updated.isLoading)
            assertNull(updated.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecipeClicked emits NavigateToDetails side effect`() = runTest(dispatcher) {
        viewModel.sideEffects.test {
            viewModel.onRecipeClicked(42)
            val effect = awaitItem()
            assertTrue(effect is FavoritesSideEffect.NavigateToDetails)
            assertEquals(42, (effect as FavoritesSideEffect.NavigateToDetails).recipeId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite error emits ShowSnackbar side effect`() = runTest(dispatcher) {
        val recipe = TestFixtures.recipe(id = 3, isFavorite = true)
        fake.emitFavorites(listOf(recipe))
        fake.failFavorite = true

        viewModel.sideEffects.test {
            viewModel.toggleFavorite(recipe)
            val effect = awaitItem()
            assertTrue(effect is FavoritesSideEffect.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissError clears error state`() = runTest(dispatcher) {
        viewModel.state.test {
            awaitItem()
            viewModel.dismissError()
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(viewModel.state.value.error)
    }
}
