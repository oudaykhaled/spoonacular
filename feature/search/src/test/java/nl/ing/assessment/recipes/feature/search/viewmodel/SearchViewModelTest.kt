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
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.core.domain.usecase.ObserveCachedRecipesUseCase
import nl.ing.assessment.recipes.core.domain.usecase.SearchRecipesUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ToggleFavoriteUseCase
import nl.ing.assessment.recipes.core.telemetry.EventTracker
import nl.ing.assessment.recipes.core.testing.FakeRecipesRepository
import nl.ing.assessment.recipes.core.testing.TestFixtures
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeRecipesRepository
    private lateinit var eventTracker: EventTracker
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRecipesRepository()
        eventTracker = mockk(relaxed = true)
        viewModel = SearchViewModel(
            searchRecipes = SearchRecipesUseCase(repository),
            observeCachedRecipes = ObserveCachedRecipesUseCase(repository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(repository),
            eventTracker = eventTracker,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSearchQueryChanged debounces query and populates recipes`() = runTest(dispatcher) {
        val fakeRecipes = TestFixtures.recipeList(count = 3)
        repository.emitCached(fakeRecipes)

        viewModel.onSearchQueryChanged("pasta")
        advanceTimeBy(400)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("pasta", state.searchInput)
        assertEquals("pasta", state.searchQuery)
        assertEquals(fakeRecipes.size, state.recipes.size)
        assertEquals(fakeRecipes.map { it.id }, state.recipes.map { it.id })
    }

    @Test
    fun `onRecipeClicked emits NavigateToDetails side effect`() = runTest(dispatcher) {
        viewModel.sideEffects.test {
            viewModel.onRecipeClicked(42)
            advanceUntilIdle()
            val effect = awaitItem()
            assertEquals(SearchSideEffect.NavigateToDetails(42), effect)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite error emits ShowSnackbar side effect`() = runTest(dispatcher) {
        val recipe = TestFixtures.recipe(id = 7)
        repository.failFavorite = true

        viewModel.sideEffects.test {
            viewModel.toggleFavorite(recipe)
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is SearchSideEffect.ShowSnackbar)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `dismissError clears the error field`() = runTest(dispatcher) {
        repository.emitCached(emptyList())
        repository.failSearch = true
        viewModel.onSearchQueryChanged("pizza")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.dismissError()
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `onSortChanged triggers a new search with the new sort`() = runTest(dispatcher) {
        val recipes = TestFixtures.recipeList(count = 2)
        repository.emitCached(recipes)
        viewModel.onSearchQueryChanged("soup")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(SortOrder.RELEVANCE, viewModel.state.value.sort)

        viewModel.onSortChanged(SortOrder.POPULARITY)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(SortOrder.POPULARITY, state.sort)
        assertEquals(recipes.size, state.recipes.size)
    }

    @Test
    fun `loadNextPage advances offset when more pages are available`() = runTest(dispatcher) {
        val recipes = TestFixtures.recipeList(count = 40)
        repository.emitCached(recipes)
        repository.fakeHasMore = true

        viewModel.onSearchQueryChanged("salad")
        advanceTimeBy(400)
        advanceUntilIdle()
        val firstOffset = viewModel.state.value.offset
        assertTrue(firstOffset > 0)

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.offset > firstOffset)
    }

    @Test
    fun `trackScreenView is called on init`() = runTest(dispatcher) {
        val nameSlot = slot<String>()
        verify { eventTracker.trackScreenView(capture(nameSlot)) }
        assertEquals("SearchScreen", nameSlot.captured)
    }
}
