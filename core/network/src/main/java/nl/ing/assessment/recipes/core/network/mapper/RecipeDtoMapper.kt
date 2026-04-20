package nl.ing.assessment.recipes.core.network.mapper

import nl.ing.assessment.recipes.core.domain.model.Ingredient
import nl.ing.assessment.recipes.core.domain.model.PageResult
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.core.domain.model.RecipeStep
import nl.ing.assessment.recipes.core.network.dto.IngredientDto
import nl.ing.assessment.recipes.core.network.dto.RecipeDetailsDto
import nl.ing.assessment.recipes.core.network.dto.RecipeSummaryDto
import nl.ing.assessment.recipes.core.network.dto.SearchResponseDto

fun RecipeSummaryDto.toDomain(cachedAt: Long): Recipe = Recipe(
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
    isFavorite = false,
    cachedAt = cachedAt
)

fun IngredientDto.toDomain(): Ingredient = Ingredient(
    id = id ?: 0,
    name = name,
    original = original,
    amount = amount,
    unit = unit,
    image = image
)

fun RecipeDetailsDto.toDomain(cachedAt: Long): RecipeDetails {
    val recipe = Recipe(
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
        isFavorite = false,
        cachedAt = cachedAt
    )
    val steps: List<RecipeStep> = analyzedInstructions
        .flatMap { it.steps }
        .map { RecipeStep(number = it.number, step = it.step) }
    return RecipeDetails(
        recipe = recipe,
        extendedIngredients = extendedIngredients.map { it.toDomain() },
        instructions = instructions,
        analyzedInstructionSteps = steps,
        sourceName = sourceName,
        servings = servings,
        cuisines = cuisines,
        dishTypes = dishTypes,
        diets = diets
    )
}

fun SearchResponseDto.toDomain(cachedAt: Long): PageResult = PageResult(
    items = results.map { it.toDomain(cachedAt) },
    totalResults = totalResults,
    offset = offset,
    hasMore = (offset + results.size) < totalResults
)
