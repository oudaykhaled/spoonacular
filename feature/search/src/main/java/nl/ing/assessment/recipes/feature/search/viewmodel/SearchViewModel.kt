package nl.ing.assessment.recipes.feature.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.ing.assessment.recipes.core.designsystem.util.UiText
import nl.ing.assessment.recipes.core.designsystem.util.toUiText
import nl.ing.assessment.recipes.core.domain.model.PageResult
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.SortOrder
import nl.ing.assessment.recipes.core.domain.usecase.ObserveCachedRecipesUseCase
import nl.ing.assessment.recipes.core.domain.usecase.SearchRecipesUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ToggleFavoriteUseCase
import nl.ing.assessment.recipes.core.telemetry.EventTracker
import java.io.IOException
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRecipes: SearchRecipesUseCase,
    private val observeCachedRecipes: ObserveCachedRecipesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val eventTracker: EventTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _sideEffects = Channel<SearchSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        eventTracker.trackScreenView("SearchScreen")
        observeCache()
        setupSearchDebounce()
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchInput = query) }
        searchQueryFlow.value = query
    }

    fun onSortChanged(sort: SortOrder) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort, offset = 0, hasMorePages = true) }
        performSearch(_state.value.searchQuery, reset = true)
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true, offset = 0, hasMorePages = true, error = null) }
        performSearch(_state.value.searchQuery, reset = true, isRefresh = true)
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMorePages) return
        if (current.searchQuery.isBlank()) return
        performSearch(current.searchQuery, reset = false)
    }

    fun onRecipeClicked(recipeId: Int) {
        viewModelScope.launch {
            _sideEffects.send(SearchSideEffect.NavigateToDetails(recipeId))
        }
    }

    fun toggleFavorite(recipe: Recipe) = toggleFavoriteInternal(recipe)

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun observeCache() {
        observeCachedRecipes()
            .distinctUntilChanged()
            .onEach { cached ->
                _state.update { current ->
                    if (current.searchQuery.isBlank()) {
                        current.copy(recipes = cached.toImmutableList())
                    } else {
                        current.copy(recipes = mergeFavoriteFlags(current.recipes, cached))
                    }
                }
            }
            .catch { e -> _state.update { it.copy(error = e.toUiText()) } }
            .launchIn(viewModelScope)
    }

    private fun setupSearchDebounce() {
        searchQueryFlow
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { query ->
                _state.update { it.copy(searchQuery = query, offset = 0, hasMorePages = true) }
                performSearch(query, reset = true)
            }
            .launchIn(viewModelScope)
    }

    private fun performSearch(query: String, reset: Boolean, isRefresh: Boolean = false) {
        if (query.isBlank()) {
            clearForBlankQuery()
            return
        }
        viewModelScope.launch {
            val startOffset = if (reset) 0 else _state.value.offset
            _state.update {
                it.copy(
                    isLoading = reset && !isRefresh,
                    isLoadingMore = !reset,
                    error = null,
                )
            }
            try {
                val result = searchRecipes(query, _state.value.sort, startOffset, PAGE_SIZE)
                _state.update { it.applySearchSuccess(result, reset, startOffset) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                handleSearchIoError(e)
            } catch (e: Exception) {
                _state.update { it.applySearchError(e.toUiText()) }
            }
        }
    }

    private fun clearForBlankQuery() {
        _state.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                isRefreshing = false,
                offset = 0,
                totalResults = 0,
                hasMorePages = true,
                error = null,
            )
        }
    }

    private fun SearchUiState.applySearchSuccess(
        result: PageResult,
        reset: Boolean,
        startOffset: Int,
    ): SearchUiState {
        val merged = if (reset) {
            result.items.toImmutableList()
        } else {
            (recipes + result.items).distinctBy { it.id }.toImmutableList()
        }
        return copy(
            recipes = merged,
            offset = startOffset + result.items.size,
            totalResults = result.totalResults,
            hasMorePages = result.hasMore,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false,
        )
    }

    private fun SearchUiState.applySearchError(error: UiText): SearchUiState = copy(
        isLoading = false,
        isLoadingMore = false,
        isRefreshing = false,
        error = error,
    )

    private suspend fun handleSearchIoError(e: IOException) {
        val current = _state.value
        if (current.recipes.isNotEmpty()) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    isRefreshing = false,
                )
            }
            _sideEffects.send(SearchSideEffect.ShowSnackbar(e.toUiText()))
        } else {
            _state.update { it.applySearchError(e.toUiText()) }
        }
    }

    private fun toggleFavoriteInternal(recipe: Recipe) {
        viewModelScope.launch {
            val previous = _state.value.recipes
            _state.update { current ->
                val optimistic = current.recipes.map {
                    if (it.id == recipe.id) it.copy(isFavorite = !recipe.isFavorite) else it
                }.toImmutableList()
                current.copy(
                    recipes = optimistic,
                    favoriteLoadingIds = (current.favoriteLoadingIds + recipe.id).toImmutableList(),
                )
            }
            try {
                toggleFavoriteUseCase(recipe.id, recipe.isFavorite)
                eventTracker.trackEvent(
                    "toggle_favorite",
                    mapOf("recipe_id" to recipe.id, "is_favorite" to !recipe.isFavorite)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(recipes = previous.toImmutableList()) }
                _sideEffects.send(SearchSideEffect.ShowSnackbar(e.toUiText()))
            } finally {
                _state.update { current ->
                    current.copy(
                        favoriteLoadingIds = current.favoriteLoadingIds
                            .filter { it != recipe.id }
                            .toImmutableList()
                    )
                }
            }
        }
    }

    private fun mergeFavoriteFlags(
        existing: kotlinx.collections.immutable.ImmutableList<Recipe>,
        cached: List<Recipe>,
    ): kotlinx.collections.immutable.ImmutableList<Recipe> {
        if (existing.isEmpty()) return existing
        val favoriteIds = cached.filter { it.isFavorite }.map { it.id }.toSet()
        return existing.map { recipe ->
            val shouldBeFavorite = recipe.id in favoriteIds
            if (recipe.isFavorite == shouldBeFavorite) recipe else recipe.copy(isFavorite = shouldBeFavorite)
        }.toImmutableList()
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        const val PAGE_SIZE = 20
        const val PREFETCH_THRESHOLD = 4
    }
}
