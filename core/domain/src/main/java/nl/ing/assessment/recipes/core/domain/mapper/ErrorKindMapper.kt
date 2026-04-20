package nl.ing.assessment.recipes.core.domain.mapper

import nl.ing.assessment.recipes.core.domain.model.ErrorKind
import nl.ing.assessment.recipes.core.domain.model.ServerException
import java.io.IOException

fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is IOException -> ErrorKind.Network
    is ServerException -> when (code) {
        RATE_LIMITED_PAYMENT_REQUIRED, RATE_LIMITED_TOO_MANY_REQUESTS -> ErrorKind.RateLimited
        else -> ErrorKind.Server
    }
    else -> ErrorKind.Unknown
}

private const val RATE_LIMITED_PAYMENT_REQUIRED = 402
private const val RATE_LIMITED_TOO_MANY_REQUESTS = 429
