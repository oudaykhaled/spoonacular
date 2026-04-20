package nl.ing.assessment.recipes.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        repeat(MAX_RETRIES + 1) { attempt ->
            if (chain.call().isCanceled()) throw IOException("Canceled")

            val response = try {
                chain.proceed(request)
            } catch (e: IOException) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    sleepCancellable(chain, backoffMillis(attempt))
                    return@repeat
                }
                throw e
            } catch (e: Exception) {
                val wrapped = IOException("Unexpected error during request", e)
                lastException = wrapped
                if (attempt < MAX_RETRIES) {
                    sleepCancellable(chain, backoffMillis(attempt))
                    return@repeat
                }
                throw wrapped
            }

            if (response.isSuccessful || response.code < SERVER_ERROR_THRESHOLD) return response
            if (attempt < MAX_RETRIES) {
                response.close()
                sleepCancellable(chain, backoffMillis(attempt))
            } else {
                return response
            }
        }

        throw lastException ?: IOException("Retry exhausted")
    }

    private fun sleepCancellable(chain: Interceptor.Chain, millis: Long) {
        var remaining = millis
        while (remaining > 0) {
            if (chain.call().isCanceled()) throw IOException("Canceled")
            val slice = if (remaining < SLEEP_SLICE_MS) remaining else SLEEP_SLICE_MS
            try {
                Thread.sleep(slice)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted", e)
            }
            remaining -= slice
        }
    }

    private fun backoffMillis(attempt: Int): Long {
        val base = INITIAL_BACKOFF_MS * (1L shl attempt)
        val jitterUpper = base / 2 + 1
        val jitter = if (jitterUpper <= 1L) 0L else Random.nextLong(0, jitterUpper)
        return base + jitter
    }

    private companion object {
        const val MAX_RETRIES = 2
        const val INITIAL_BACKOFF_MS = 500L
        const val SERVER_ERROR_THRESHOLD = 500
        const val SLEEP_SLICE_MS = 50L
    }
}
