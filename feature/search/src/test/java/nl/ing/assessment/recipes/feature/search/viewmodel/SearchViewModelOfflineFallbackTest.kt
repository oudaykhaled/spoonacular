package nl.ing.assessment.recipes.feature.search.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.ing.assessment.recipes.core.domain.usecase.ObserveCachedRecipesUseCase
import nl.ing.assessment.recipes.core.domain.usecase.SearchRecipesUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ToggleFavoriteUseCase
import nl.ing.assessment.recipes.core.telemetry.NoOpEventTracker
import nl.ing.assessment.recipes.core.testing.FakeRecipesRepository
import nl.ing.assessment.recipes.core.testing.TestFixtures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelOfflineFallbackTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRecipesRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRecipesRepository()
        viewModel = SearchViewModel(
            searchRecipes = SearchRecipesUseCase(fakeRepository),
            observeCachedRecipes = ObserveCachedRecipesUseCase(fakeRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepository),
            eventTracker = NoOpEventTracker(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `offline with cached results emits snackbar and keeps recipes`() = runTest(testDispatcher) {
        val cached = listOf(
            TestFixtures.recipe(id = 1, title = "Cached pasta"),
            TestFixtures.recipe(id = 2, title = "Cached pizza"),
        )
        fakeRepository.emitCached(cached)
        advanceUntilIdle()

        fakeRepository.failSearch = true

        viewModel.sideEffects.test {
            viewModel.onSearchQueryChanged("pasta")
            advanceTimeBy(400)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(
                "expected ShowSnackbar but got $effect",
                effect is SearchSideEffect.ShowSnackbar
            )
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.state.value
        assertEquals(2, state.recipes.size)
        assertNull(state.error)
    }
}
