package nl.ing.assessment.recipes.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || response.code < SERVER_ERROR_THRESHOLD) return response
                if (attempt < MAX_RETRIES) {
                    response.close()
                    Thread.sleep(backoffMillis(attempt))
                } else {
                    return response
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(backoffMillis(attempt))
                }
            } catch (e: Exception) {
                lastException = IOException("Unexpected error during request", e)
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(backoffMillis(attempt))
                }
            }
        }

        throw lastException ?: IOException("Retry exhausted")
    }

    private fun backoffMillis(attempt: Int): Long = INITIAL_BACKOFF_MS * (1L shl attempt)

    private companion object {
        const val MAX_RETRIES = 2
        const val INITIAL_BACKOFF_MS = 500L
        const val SERVER_ERROR_THRESHOLD = 500
    }
}
