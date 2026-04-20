package nl.ing.assessment.recipes.core.network.di

import nl.ing.assessment.recipes.core.network.interceptor.InterceptorEntry
import nl.ing.assessment.recipes.core.network.interceptor.InterceptorType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DebugInterceptorModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideHttpLoggingEntry(): InterceptorEntry {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return InterceptorEntry(
            order = LOGGING_ORDER,
            interceptor = logging,
            type = InterceptorType.NETWORK
        )
    }

    private const val LOGGING_ORDER = 100
}
