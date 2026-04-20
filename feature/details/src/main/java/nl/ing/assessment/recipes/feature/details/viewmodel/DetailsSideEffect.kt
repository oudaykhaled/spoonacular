package nl.ing.assessment.recipes.feature.details.viewmodel

import nl.ing.assessment.recipes.core.designsystem.util.UiText

sealed interface DetailsSideEffect {
    data class ShowSnackbar(val message: UiText) : DetailsSideEffect
    data class OpenUrl(val url: String) : DetailsSideEffect
}
