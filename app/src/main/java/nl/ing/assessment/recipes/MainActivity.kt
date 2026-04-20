package nl.ing.assessment.recipes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import nl.ing.assessment.recipes.core.designsystem.theme.RecipesTheme
import nl.ing.assessment.recipes.core.domain.model.ThemeMode
import nl.ing.assessment.recipes.core.domain.repository.SettingsRepository
import nl.ing.assessment.recipes.navigation.AppNavigation
import nl.ing.assessment.recipes.navigation.DeepLinkParser

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialDeepLinkRecipeId = extractDeepLinkRecipeId(intent)

        setContent {
            @OptIn(ExperimentalComposeUiApi::class)
            Box(
                modifier = Modifier.semantics { testTagsAsResourceId = true }
            ) {
                MainContent(initialDeepLinkRecipeId = initialDeepLinkRecipeId)
            }
        }
    }

    @Composable
    private fun MainContent(initialDeepLinkRecipeId: Int?) {
        val themeMode by settingsRepository.themeMode
            .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
        val dynamicColor by settingsRepository.dynamicColor
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
