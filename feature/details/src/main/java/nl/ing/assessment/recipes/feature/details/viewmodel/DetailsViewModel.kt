package nl.ing.assessment.recipes.feature.details.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.ing.assessment.recipes.core.designsystem.util.toUiText
import nl.ing.assessment.recipes.core.domain.usecase.GetRecipeDetailsUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ObserveRecipeDetailsUseCase
import nl.ing.assessment.recipes.core.domain.usecase.ToggleFavoriteUseCase
import nl.ing.assessment.recipes.core.telemetry.EventTracker

@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
class DetailsViewModel @AssistedInject constructor(
    @Assisted val recipeId: Int,
    private val getRecipeDetails: GetRecipeDetailsUseCase,
    private val observeRecipeDetails: ObserveRecipeDetailsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val eventTracker: EventTracker,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(recipeId: Int): DetailsViewModel
    }

    private val _state = MutableStateFlow(DetailsUiState())
    val state = _state.asStateFlow()

    private val _sideEffects = Channel<DetailsSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    init {
        eventTracker.trackScreenView(SCREEN_NAME)
        observeCachedDetails()
        fetchDetails()
    }

    fun retry() {
        fetchDetails()
    }

    fun toggleFavorite() {
        toggleFavoriteInternal()
    }

    fun onOpenSource() {
        val url = _state.value.details?.recipe?.sourceUrl ?: return
        viewModelScope.launch { _sideEffects.send(DetailsSideEffect.OpenUrl(url)) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun observeCachedDetails() {
        observeRecipeDetails(recipeId)
            .distinctUntilChanged()
            .onEach { cached ->
                if (cached != null) {
                    _state.update { current -> current.copy(details = cached) }
                } else {
                    // Cache miss or eviction — trigger a fresh network fetch
                    fetchDetails()
                }
            }
            .catch { e -> _state.update { it.copy(error = e.toUiText()) } }
            .launchIn(viewModelScope)
    }

    private fun fetchDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val details = getRecipeDetails(recipeId)
                _state.update { it.copy(details = details, isLoading = false) }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.toUiText()) }
            }
        }
    }

    private fun toggleFavoriteInternal() {
        val recipe = _state.value.details?.recipe ?: return
        viewModelScope.launch {
            _state.update { it.copy(isFavoriteLoading = true) }
            try {
                toggleFavoriteUseCase(recipe.id, recipe.isFavorite)
                _state.update { it.copy(isFavoriteLoading = false) }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _state.update { it.copy(isFavoriteLoading = false) }
                _sideEffects.send(DetailsSideEffect.ShowSnackbar(e.toUiText()))
            }
        }
    }

    private companion object {
        const val SCREEN_NAME = "DetailsScreen"
    }
}
