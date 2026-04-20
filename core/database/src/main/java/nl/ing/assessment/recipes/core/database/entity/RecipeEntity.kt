package nl.ing.assessment.recipes.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val image: String? = null,
    val imageType: String? = null,
    val summary: String? = null,
    val readyInMinutes: Int? = null,
    val healthScore: Double? = null,
    val aggregateLikes: Int? = null,
    val pricePerServing: Double? = null,
    val sourceUrl: String? = null,
    val sourceName: String? = null,
    val servings: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val hasDetails: Boolean = false,
    @ColumnInfo(defaultValue = "[]")
    val cuisinesJson: String = "[]",
    @ColumnInfo(defaultValue = "[]")
    val dishTypesJson: String = "[]",
    @ColumnInfo(defaultValue = "[]")
    val dietsJson: String = "[]",
    val instructions: String? = null,
    val cachedAt: Long
)
