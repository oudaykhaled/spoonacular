package nl.ing.assessment.recipes.di

import dagger.Binds
import dagger.Module
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
abstract class FakeTestRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFakeRecipesRepository(impl: FakeRecipesRepository): RecipesRepository
}
