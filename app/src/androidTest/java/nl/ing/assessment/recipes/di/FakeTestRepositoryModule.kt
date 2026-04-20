package nl.ing.assessment.recipes.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import nl.ing.assessment.recipes.core.database.di.RepositoryModule
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import nl.ing.assessment.recipes.core.testing.FakeRecipesRepository
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class],
)
object FakeTestRepositoryModule {

    @Provides
    @Singleton
    fun provideFakeRecipesRepository(): RecipesRepository = FakeRecipesRepository()
}
