package nl.ing.assessment.recipes.core.designsystem.util

import nl.ing.assessment.recipes.core.designsystem.R
import nl.ing.assessment.recipes.core.domain.mapper.toErrorKind
import nl.ing.assessment.recipes.core.domain.model.ErrorKind

fun Throwable.toUiText(): UiText = when (toErrorKind()) {
    ErrorKind.Network -> UiText.Resource(R.string.error_network)
    ErrorKind.Server -> UiText.Resource(R.string.error_server)
    ErrorKind.RateLimited -> UiText.Resource(R.string.error_rate_limited)
    ErrorKind.Unknown -> UiText.Resource(R.string.error_unknown)
}
