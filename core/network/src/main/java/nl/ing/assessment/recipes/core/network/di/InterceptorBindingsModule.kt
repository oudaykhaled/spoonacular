package nl.ing.assessment.recipes.core.network.di

import nl.ing.assessment.recipes.core.network.interceptor.ApiKeyInterceptor
import nl.ing.assessment.recipes.core.network.interceptor.CommonHeadersInterceptor
import nl.ing.assessment.recipes.core.network.interceptor.InterceptorEntry
import nl.ing.assessment.recipes.core.network.interceptor.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InterceptorBindingsModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideApiKeyEntry(interceptor: ApiKeyInterceptor): InterceptorEntry =
        InterceptorEntry(order = API_KEY_ORDER, interceptor = interceptor)

    @Provides
    @IntoSet
    @Singleton
    fun provideCommonHeadersEntry(interceptor: CommonHeadersInterceptor): InterceptorEntry =
        InterceptorEntry(order = COMMON_HEADERS_ORDER, interceptor = interceptor)

    @Provides
    @IntoSet
    @Singleton
    fun provideRetryEntry(interceptor: RetryInterceptor): InterceptorEntry =
        InterceptorEntry(order = RETRY_ORDER, interceptor = interceptor)

    private const val API_KEY_ORDER = 10
    private const val COMMON_HEADERS_ORDER = 20
    private const val RETRY_ORDER = 30
}
