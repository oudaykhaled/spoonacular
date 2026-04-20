package nl.ing.assessment.recipes.core.database.mapper

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import nl.ing.assessment.recipes.core.database.entity.IngredientEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeStepEntity
import nl.ing.assessment.recipes.core.database.entity.RecipeWithRelations
import nl.ing.assessment.recipes.core.domain.model.Ingredient
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.core.domain.model.RecipeStep

private val stringListSerializer = ListSerializer(String.serializer())

fun RecipeEntity.toDomain(): Recipe = Recipe(
    id = id,
    title = title,
    image = image,
    imageType = imageType,
    summary = summary,
    readyInMinutes = readyInMinutes,
    healthScore = healthScore,
    aggregateLikes = aggregateLikes,
    pricePerServing = pricePerServing,
    sourceUrl = sourceUrl,
    isFavorite = isFavorite,
    cachedAt = cachedAt
)

fun IngredientEntity.toDomain(): Ingredient = Ingredient(
    id = externalId,
    name = name,
    original = original,
    amount = amount,
    unit = unit,
    image = image
)

fun RecipeStepEntity.toDomain(): RecipeStep = RecipeStep(
    number = number,
    step = step
)

fun RecipeWithRelations.toDomainDetails(json: Json): RecipeDetails {
    val cuisines = runCatching {
        json.decodeFromString(stringListSerializer, recipe.cuisinesJson)
    }.getOrDefault(emptyList())
    val dishTypes = runCatching {
        json.decodeFromString(stringListSerializer, recipe.dishTypesJson)
    }.getOrDefault(emptyList())
    val diets = runCatching {
        json.decodeFromString(stringListSerializer, recipe.dietsJson)
    }.getOrDefault(emptyList())
    return RecipeDetails(
        recipe = recipe.toDomain(),
        extendedIngredients = ingredients.map { it.toDomain() },
        instructions = recipe.instructions,
        analyzedInstructionSteps = steps.sortedBy { it.number }.map { it.toDomain() },
        sourceName = recipe.sourceName,
        servings = recipe.servings,
        cuisines = cuisines,
        dishTypes = dishTypes,
        diets = diets
    )
}

@Suppress("LongParameterList")
fun Recipe.toEntity(
    cachedAt: Long,
    hasDetails: Boolean = false,
    json: Json,
    cuisines: List<String> = emptyList(),
    dishTypes: List<String> = emptyList(),
    diets: List<String> = emptyList(),
    instructions: String? = null,
    sourceName: String? = null,
    servings: Int? = null
): RecipeEntity = RecipeEntity(
    id = id,
    title = title,
    image = image,
    imageType = imageType,
    summary = summary,
    readyInMinutes = readyInMinutes,
    healthScore = healthScore,
    aggregateLikes = aggregateLikes,
    pricePerServing = pricePerServing,
    sourceUrl = sourceUrl,
    sourceName = sourceName,
    servings = servings,
    isFavorite = isFavorite,
    hasDetails = hasDetails,
    cuisinesJson = json.encodeToString(stringListSerializer, cuisines),
    dishTypesJson = json.encodeToString(stringListSerializer, dishTypes),
    dietsJson = json.encodeToString(stringListSerializer, diets),
    instructions = instructions,
    cachedAt = cachedAt
)

fun Ingredient.toEntity(recipeId: Int): IngredientEntity = IngredientEntity(
    rowId = 0,
    recipeId = recipeId,
    externalId = id,
    name = name,
    original = original,
    amount = amount,
    unit = unit,
    image = image
)

fun RecipeStep.toEntity(recipeId: Int): RecipeStepEntity = RecipeStepEntity(
    rowId = 0,
    recipeId = recipeId,
    number = number,
    step = step
)

internal fun encodeStringList(json: Json, values: List<String>): String =
    json.encodeToString(stringListSerializer, values)
