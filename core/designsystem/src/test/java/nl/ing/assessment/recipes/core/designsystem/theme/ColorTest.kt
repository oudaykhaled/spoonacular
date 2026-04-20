package nl.ing.assessment.recipes.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorTest {

    @Test
    fun `ING orange Primary40 equals reference Color(0xFFFF6200)`() {
        assertEquals(Color(0xFFFF6200), Primary40)
    }

    @Test
    fun `Primary40 ARGB value equals 0xFFFF6200`() {
        assertEquals(0xFFFF6200.toInt(), Primary40.toArgb())
    }
}
