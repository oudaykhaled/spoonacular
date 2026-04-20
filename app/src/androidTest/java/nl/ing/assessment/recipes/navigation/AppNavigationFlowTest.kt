package nl.ing.assessment.recipes.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import nl.ing.assessment.recipes.MainActivity
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AppNavigationFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivityLaunches() {
        hiltRule.inject()
        composeRule.waitForIdle()
    }
}
