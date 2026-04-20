package nl.ing.assessment.recipes.core.domain.model

import androidx.compose.runtime.Stable

@Stable
enum class ErrorKind {
    Network,
    Server,
    RateLimited,
    Unknown
}
