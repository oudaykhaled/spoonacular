package nl.ing.assessment.recipes.core.telemetry.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.ing.assessment.recipes.core.telemetry.EventTracker
import nl.ing.assessment.recipes.core.telemetry.NoOpEventTracker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TelemetryModule {
    @Binds
    @Singleton
    abstract fun bindEventTracker(impl: NoOpEventTracker): EventTracker
}
