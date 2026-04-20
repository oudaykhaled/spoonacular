package nl.ing.assessment.recipes.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Search : AppRoute

    @Serializable
    data class Details(val recipeId: Int) : AppRoute

    @Serializable
    data object Favorites : AppRoute

    @Serializable
    data object Settings : AppRoute
}
