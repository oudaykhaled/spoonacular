package nl.ing.assessment.recipes.core.designsystem.util

import nl.ing.assessment.recipes.core.designsystem.R
import nl.ing.assessment.recipes.core.domain.model.ServerException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ErrorUiMapperTest {

    @Test
    fun `IOException maps to error_network`() {
        val uiText = IOException("offline").toUiText() as UiText.Resource
        assertEquals(R.string.error_network, uiText.resId)
    }

    @Test
    fun `ServerException with code 500 maps to error_server`() {
        val uiText = ServerException(code = 500, message = "boom").toUiText() as UiText.Resource
        assertEquals(R.string.error_server, uiText.resId)
    }

    @Test
    fun `ServerException with code 402 maps to error_rate_limited`() {
        val uiText = ServerException(code = 402, message = "payment required").toUiText() as UiText.Resource
        assertEquals(R.string.error_rate_limited, uiText.resId)
    }

    @Test
    fun `ServerException with code 429 maps to error_rate_limited`() {
        val uiText = ServerException(code = 429, message = "too many").toUiText() as UiText.Resource
        assertEquals(R.string.error_rate_limited, uiText.resId)
    }

    @Test
    fun `IllegalStateException maps to error_unknown`() {
        val uiText = IllegalStateException("oops").toUiText() as UiText.Resource
        assertEquals(R.string.error_unknown, uiText.resId)
    }
}
