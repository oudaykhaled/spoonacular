package nl.ing.assessment.recipes.core.testing

import nl.ing.assessment.recipes.core.domain.model.Ingredient
import nl.ing.assessment.recipes.core.domain.model.Recipe
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.core.domain.model.RecipeStep

object TestFixtures {

    fun recipe(
        id: Int = 1,
        title: String = "Pasta Carbonara",
        image: String? = "https://example.com/pasta.jpg",
        imageType: String? = "jpg",
        summary: String? = "A classic Italian pasta dish.",
        readyInMinutes: Int? = 30,
        healthScore: Double? = 65.0,
        aggregateLikes: Int? = 120,
        pricePerServing: Double? = 2.5,
        sourceUrl: String? = "https://example.com/recipe/1",
        isFavorite: Boolean = false,
        cachedAt: Long = 0L
    ) = Recipe(
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

    fun recipeList(count: Int = 5): List<Recipe> =
        (1..count).map { i ->
            recipe(
                id = i,
                title = "Recipe $i",
                summary = "Summary for recipe $i"
            )
        }

    fun ingredient(
        id: Int = 1,
        name: String = "spaghetti",
        original: String = "200g spaghetti",
        amount: Double = 200.0,
        unit: String = "g",
        image: String? = null
    ) = Ingredient(
        id = id,
        name = name,
        original = original,
        amount = amount,
        unit = unit,
        image = image
    )

    fun recipeStep(
        number: Int = 1,
        step: String = "Boil water."
    ) = RecipeStep(
        number = number,
        step = step
    )

    fun recipeDetails(
        id: Int = 1,
        recipe: Recipe = recipe(id = id),
        extendedIngredients: List<Ingredient> = listOf(ingredient()),
        instructions: String? = "Cook pasta. Add sauce. Serve.",
        analyzedInstructionSteps: List<RecipeStep> = listOf(recipeStep()),
        sourceName: String? = "Example Source",
        servings: Int? = 2,
        cuisines: List<String> = listOf("Italian"),
        dishTypes: List<String> = listOf("main course"),
        diets: List<String> = emptyList()
    ) = RecipeDetails(
        recipe = recipe,
        extendedIngredients = extendedIngredients,
        instructions = instructions,
        analyzedInstructionSteps = analyzedInstructionSteps,
        sourceName = sourceName,
        servings = servings,
        cuisines = cuisines,
        dishTypes = dishTypes,
        diets = diets
    )
}
