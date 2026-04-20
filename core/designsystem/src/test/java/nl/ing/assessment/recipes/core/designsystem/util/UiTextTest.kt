package nl.ing.assessment.recipes.core.designsystem.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import nl.ing.assessment.recipes.core.designsystem.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UiTextTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `Raw asString returns underlying value`() {
        val text = UiText.Raw("hello")
        assertEquals("hello", text.asString(context))
    }

    @Test
    fun `Resource asString with no args resolves resource`() {
        val text = UiText.Resource(R.string.error_retry)
        assertEquals(context.getString(R.string.error_retry), text.asString(context))
    }

    @Test
    fun `Resource asString with args formats correctly`() {
        val text = UiText.Resource(R.string.minutes_format, listOf(42))
        assertEquals("42 min", text.asString(context))
    }

    @Test
    fun `Resource asString with health score args formats correctly`() {
        val text = UiText.Resource(R.string.health_score_format, listOf(85))
        assertEquals("Health 85", text.asString(context))
    }
}
