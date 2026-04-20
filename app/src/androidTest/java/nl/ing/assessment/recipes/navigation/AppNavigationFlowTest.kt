package nl.ing.assessment.recipes.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import nl.ing.assessment.recipes.MainActivity
import nl.ing.assessment.recipes.feature.favorites.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AppNavigationFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun app_launches_on_search_and_nav_favorites_opens_favorites_list() {
        composeRule.onNodeWithTag("search_bar").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_favorites").performClick()

        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithTag("bottom_navigation_bar").fetchSemanticsNodes().isNotEmpty()
        }

        val favoritesTitle = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.favorites_title)
        composeRule.onNodeWithText(favoritesTitle).assertIsDisplayed()
    }
}
