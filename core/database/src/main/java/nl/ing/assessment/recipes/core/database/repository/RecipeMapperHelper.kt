package nl.ing.assessment.recipes.core.database.repository

import kotlinx.serialization.json.Json
import nl.ing.assessment.recipes.core.database.entity.RecipeEntity
import nl.ing.assessment.recipes.core.database.mapper.encodeStringList
import nl.ing.assessment.recipes.core.network.dto.RecipeDetailsDto
import nl.ing.assessment.recipes.core.network.dto.RecipeSummaryDto

internal object RecipeMapperHelper {

    fun summaryToEntity(dto: RecipeSummaryDto, cachedAt: Long, json: Json): RecipeEntity =
        RecipeEntity(
            id = dto.id,
            title = dto.title,
            image = dto.image,
            imageType = dto.imageType,
            summary = dto.summary,
            readyInMinutes = dto.readyInMinutes,
            healthScore = dto.healthScore,
            aggregateLikes = dto.aggregateLikes,
            pricePerServing = dto.pricePerServing,
            sourceUrl = dto.sourceUrl,
            sourceName = dto.sourceName,
            servings = dto.servings,
            isFavorite = false,
            hasDetails = false,
            cuisinesJson = encodeStringList(json, emptyList()),
            dishTypesJson = encodeStringList(json, emptyList()),
            dietsJson = encodeStringList(json, emptyList()),
            instructions = null,
            cachedAt = cachedAt
        )

    fun detailsToEntity(dto: RecipeDetailsDto, cachedAt: Long, json: Json): RecipeEntity =
        RecipeEntity(
            id = dto.id,
            title = dto.title,
            image = dto.image,
            imageType = dto.imageType,
            summary = dto.summary,
            readyInMinutes = dto.readyInMinutes,
            healthScore = dto.healthScore,
            aggregateLikes = dto.aggregateLikes,
            pricePerServing = dto.pricePerServing,
            sourceUrl = dto.sourceUrl,
            sourceName = dto.sourceName,
            servings = dto.servings,
            isFavorite = false,
            hasDetails = true,
            cuisinesJson = encodeStringList(json, dto.cuisines),
            dishTypesJson = encodeStringList(json, dto.dishTypes),
            dietsJson = encodeStringList(json, dto.diets),
            instructions = dto.instructions,
            cachedAt = cachedAt
        )
}
