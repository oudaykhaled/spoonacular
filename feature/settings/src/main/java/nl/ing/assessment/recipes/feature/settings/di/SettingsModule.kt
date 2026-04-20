package nl.ing.assessment.recipes.feature.settings.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import nl.ing.assessment.recipes.feature.settings.data.SettingsRepository
import nl.ing.assessment.recipes.feature.settings.data.SettingsRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
