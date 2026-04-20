package nl.ing.assessment.recipes.core.domain.mapper

import nl.ing.assessment.recipes.core.domain.model.ErrorKind
import nl.ing.assessment.recipes.core.domain.model.ServerException
import java.io.IOException

fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is IOException -> ErrorKind.Network
    is ServerException -> when (code) {
        RateLimitedPaymentRequired, RateLimitedTooManyRequests -> ErrorKind.RateLimited
        else -> ErrorKind.Server
    }
    else -> ErrorKind.Unknown
}

private const val RateLimitedPaymentRequired = 402
private const val RateLimitedTooManyRequests = 429
