package nl.ing.assessment.recipes.feature.favorites.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.ing.assessment.recipes.core.designsystem.util.toUiText
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.usecase.ObserveFavoritesUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ToggleFavoriteUseCase
import nl.ing.assessment.recipes.core.telemetry.EventTracker
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    eventTracker: EventTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private val _sideEffects = Channel<FavoritesSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<FavoritesSideEffect> = _sideEffects.receiveAsFlow()

    init {
        eventTracker.trackScreenView("FavoritesScreen")
        observeFavoritesInternal()
    }

    fun onRecipeClicked(id: Int) {
        viewModelScope.launch {
            _sideEffects.send(FavoritesSideEffect.NavigateToDetails(id))
        }
    }

    fun toggleFavorite(recipe: Recipe) = toggleFavoriteInternal(recipe)

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun observeFavoritesInternal() {
        observeFavoritesUseCase()
            .distinctUntilChanged()
            .map<List<Recipe>, ImmutableList<Recipe>> { it.toImmutableList() }
            .onEach { list ->
                _state.update { it.copy(favorites = list, isLoading = false) }
            }
            .catch { e ->
                _state.update { it.copy(error = e.toUiText(), isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun toggleFavoriteInternal(recipe: Recipe) {
        viewModelScope.launch {
            val previous = _state.value.favorites
            _state.update { current ->
                val optimistic = current.favorites.map {
                    if (it.id == recipe.id) it.copy(isFavorite = !recipe.isFavorite) else it
                }.toImmutableList()
                current.copy(favorites = optimistic)
            }
            markToggleLoading(recipe.id, loading = true)
            try {
                toggleFavoriteUseCase(recipe.id, recipe.isFavorite)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(favorites = previous.toImmutableList()) }
                _sideEffects.send(FavoritesSideEffect.ShowSnackbar(e.toUiText()))
            } finally {
                markToggleLoading(recipe.id, loading = false)
            }
        }
    }

    private fun markToggleLoading(id: Int, loading: Boolean) {
        _state.update { current ->
            val updated = if (loading) {
                current.toggleLoadingIds + id
            } else {
                current.toggleLoadingIds.filterNot { it == id }
            }
            current.copy(toggleLoadingIds = updated.toImmutableList())
        }
    }
}
