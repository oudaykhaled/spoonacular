package nl.ing.assessment.recipes.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RecipeWithRelations(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val steps: List<RecipeStepEntity>
)
