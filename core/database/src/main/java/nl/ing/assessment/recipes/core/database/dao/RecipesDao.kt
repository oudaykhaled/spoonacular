package nl.ing.assessment.recipes.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import nl.ing.assessment.recipes.core.database.entity.IngredientEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeStepEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeWithRelations

@Dao
abstract class RecipesDao {

    @Query("SELECT * FROM recipes ORDER BY cachedAt DESC")
    abstract fun observeAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY cachedAt DESC")
    abstract fun observeFavorites(): Flow<List<RecipeEntity>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract fun observeRecipeWithRelations(id: Int): Flow<RecipeWithRelations?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun getRecipeWithRelations(id: Int): RecipeWithRelations?

    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun getRecipe(id: Int): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRecipes(items: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertIngredients(items: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSteps(items: List<RecipeStepEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :id")
    abstract suspend fun clearIngredients(id: Int)

    @Query("DELETE FROM recipe_steps WHERE recipeId = :id")
    abstract suspend fun clearSteps(id: Int)

    @Query("UPDATE recipes SET isFavorite = :isFav WHERE id = :id")
    abstract suspend fun setFavorite(id: Int, isFav: Boolean)

    @Query("SELECT id FROM recipes WHERE isFavorite = 1")
    abstract suspend fun getFavoriteIds(): List<Int>

    @Transaction
    open suspend fun upsertSearchResults(items: List<RecipeEntity>) {
        val favoriteIds = getFavoriteIds().toSet()
        val merged = items.map { row ->
            if (row.id in favoriteIds) row.copy(isFavorite = true) else row
        }
        upsertRecipes(merged)
    }

    @Transaction
    open suspend fun upsertRecipeDetails(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        steps: List<RecipeStepEntity>
    ) {
        val existing = getRecipe(recipe.id)
        val merged = if (existing != null && existing.isFavorite) {
            recipe.copy(isFavorite = true)
        } else {
            recipe
        }
        upsertRecipes(listOf(merged))
        clearIngredients(recipe.id)
        clearSteps(recipe.id)
        insertIngredients(ingredients)
        insertSteps(steps)
    }
}
