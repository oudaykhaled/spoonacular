package nl.ing.assessment.recipes.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import nl.ing.assessment.recipes.core.database.dao.RecipesDao
import nl.ing.assessment.recipes.core.database.entity.IngredientEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeStepEntity

@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        RecipeStepEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RecipesDatabase : RoomDatabase() {
    abstract fun recipesDao(): RecipesDao
}
