package nl.ing.assessment.recipes.core.network.interceptor

import nl.ing.assessment.recipes.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyInterceptor @Inject constructor() : Interceptor {

    private val apiKey: String = BuildConfig.SPOONACULAR_API_KEY

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url

        val hasApiKey = originalUrl.queryParameter(QUERY_API_KEY) != null
        if (apiKey.isEmpty() || hasApiKey) {
            return chain.proceed(original)
        }

        val newUrl = originalUrl.newBuilder()
            .addQueryParameter(QUERY_API_KEY, apiKey)
            .build()

        val newRequest = original.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }

    private companion object {
        const val QUERY_API_KEY = "apiKey"
    }
}
