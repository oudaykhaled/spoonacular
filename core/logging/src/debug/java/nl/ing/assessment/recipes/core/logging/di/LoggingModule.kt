package nl.ing.assessment.recipes.core.logging.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.ing.assessment.recipes.core.logging.Logger
import nl.ing.assessment.recipes.core.logging.TimberLogger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {
    @Binds
    @Singleton
    abstract fun bindLogger(impl: TimberLogger): Logger
}
