package nl.ing.assessment.recipes.core.database.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.ing.assessment.recipes.core.database.repository.RecipesRepositoryImpl
import nl.ing.assessment.recipes.core.domain.repository.RecipesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecipesRepository(impl: RecipesRepositoryImpl): RecipesRepository
}
