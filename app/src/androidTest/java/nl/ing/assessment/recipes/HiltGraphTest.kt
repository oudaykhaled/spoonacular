package nl.ing.assessment.recipes

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HiltGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun hiltGraphCompiles() {
        hiltRule.inject()
    }
}
