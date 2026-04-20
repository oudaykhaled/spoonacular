package nl.ing.assessment.recipes.core.network.api

import nl.ing.assessment.recipes.core.network.dto.RecipeDetailsDto
import nl.ing.assessment.recipes.core.network.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularApi {
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("sort") sort: String? = null,
        @Query("sortDirection") sortDirection: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("number") number: Int = 20,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true
    ): SearchResponseDto

    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") id: Int,
        @Query("includeNutrition") includeNutrition: Boolean = false
    ): RecipeDetailsDto
}
