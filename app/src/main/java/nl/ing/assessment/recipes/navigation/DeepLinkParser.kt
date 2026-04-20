package nl.ing.assessment.recipes.navigation

import android.net.Uri

object DeepLinkParser {
    /** Parses `recipes://recipe/<id>` and returns the numeric id or null. */
    fun parseRecipeDeepLink(uri: Uri?): Int? {
        if (uri == null || uri.scheme != "recipes" || uri.host != "recipe") return null
        val last = uri.pathSegments.firstOrNull() ?: return null
        return last.toIntOrNull()
    }
}
