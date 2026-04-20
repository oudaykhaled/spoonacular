package nl.ing.assessment.recipes.core.network.interceptor

import nl.ing.assessment.recipes.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonHeadersInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "IngRecipes/${BuildConfig.VERSION_NAME}")
            .build()
        return chain.proceed(request)
    }
}
