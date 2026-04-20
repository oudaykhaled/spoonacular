package nl.ing.assessment.recipes

import android.net.Uri
import nl.ing.assessment.recipes.navigation.DeepLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkParsingTest {

    @Test
    fun `valid deep link returns recipe id`() {
        val uri = Uri.parse("recipes://recipe/42")
        assertEquals(42, DeepLinkParser.parseRecipeDeepLink(uri))
    }

    @Test
    fun `null uri returns null`() {
        assertNull(DeepLinkParser.parseRecipeDeepLink(null))
    }

    @Test
    fun `wrong scheme returns null`() {
        val uri = Uri.parse("https://recipe/42")
        assertNull(DeepLinkParser.parseRecipeDeepLink(uri))
    }

    @Test
    fun `wrong host returns null`() {
        val uri = Uri.parse("recipes://recipes/42")
        assertNull(DeepLinkParser.parseRecipeDeepLink(uri))
    }

    @Test
    fun `non-numeric id returns null`() {
        val uri = Uri.parse("recipes://recipe/abc")
        assertNull(DeepLinkParser.parseRecipeDeepLink(uri))
    }

    @Test
    fun `no path segment returns null`() {
        val uri = Uri.parse("recipes://recipe")
        assertNull(DeepLinkParser.parseRecipeDeepLink(uri))
    }
}
