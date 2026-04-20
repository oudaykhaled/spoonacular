package nl.ing.assessment.recipes.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.ing.assessment.recipes.core.database.RecipesDatabase
import nl.ing.assessment.recipes.core.database.dao.RecipesDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRecipesDatabase(
        @ApplicationContext context: Context
    ): RecipesDatabase =
        Room.databaseBuilder(
            context,
            RecipesDatabase::class.java,
            "recipes.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideRecipesDao(database: RecipesDatabase): RecipesDao = database.recipesDao()
}
