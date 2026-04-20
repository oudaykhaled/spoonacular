package nl.ing.assessment.recipes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.core.designsystem.theme.ThemeMode
import nl.ing.assessment.recipes.navigation.AppNavigation
import nl.ing.assessment.recipes.navigation.DeepLinkParser
import nl.ing.assessment.recipes.settings.SettingsManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialDeepLinkRecipeId = extractDeepLinkRecipeId(intent)

        setContent {
            MainContent(initialDeepLinkRecipeId = initialDeepLinkRecipeId)
        }
    }

    @Composable
    private fun MainContent(initialDeepLinkRecipeId: Int?) {
        val themeMode by settingsManager.themeMode
            .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
        val dynamicColor by settingsManager.dynamicColor
            .collectAsStateWithLifecycle(initialValue = false)

        RecipesTheme(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
        ) {
            AppNavigation(initialDeepLinkRecipeId = initialDeepLinkRecipeId)
        }
    }

    private fun extractDeepLinkRecipeId(intent: Intent?): Int? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return DeepLinkParser.parseRecipeDeepLink(intent.data)
    }
}
