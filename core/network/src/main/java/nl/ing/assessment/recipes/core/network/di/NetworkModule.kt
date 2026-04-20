package nl.ing.assessment.recipes.core.network.di

import nl.ing.assessment.recipes.core.network.BuildConfig
import nl.ing.assessment.recipes.core.network.api.SpoonacularApi
import nl.ing.assessment.recipes.core.network.interceptor.InterceptorEntry
import nl.ing.assessment.recipes.core.network.interceptor.InterceptorType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        entries: Set<@JvmSuppressWildcards InterceptorEntry>
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        val sorted = entries.sorted()
        sorted.filter { it.type == InterceptorType.APPLICATION }.forEach {
            builder.addInterceptor(it.interceptor)
        }
        sorted.filter { it.type == InterceptorType.NETWORK }.forEach {
            builder.addNetworkInterceptor(it.interceptor)
        }

        // Pin SPKI hashes for api.spoonacular.com (leaf). Update if Spoonacular rotates certs;
        // add a second pin from the issuing CA for safer rotation windows.
        val pinner = CertificatePinner.Builder()
            .add("api.spoonacular.com", "sha256/ilij17Y3nJ2GgpIPlEQY7ZJVtAggSxEcJEnkr0a0hzo=")
            .build()
        builder.certificatePinner(pinner)

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideSpoonacularApi(retrofit: Retrofit): SpoonacularApi =
        retrofit.create(SpoonacularApi::class.java)

    private const val TIMEOUT_SECONDS = 15L
}
