package nl.ing.assessment.recipes.core.domain.mapper

import nl.ing.assessment.recipes.core.domain.model.ErrorKind
import nl.ing.assessment.recipes.core.domain.model.ServerException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ErrorKindMapperTest {

    @Test
    fun `IOException maps to Network`() {
        assertEquals(ErrorKind.Network, IOException("down").toErrorKind())
    }

    @Test
    fun `ServerException with 402 maps to RateLimited`() {
        assertEquals(ErrorKind.RateLimited, ServerException(402, "payment").toErrorKind())
    }

    @Test
    fun `ServerException with 429 maps to RateLimited`() {
        assertEquals(ErrorKind.RateLimited, ServerException(429, "too many").toErrorKind())
    }

    @Test
    fun `ServerException with other code maps to Server`() {
        assertEquals(ErrorKind.Server, ServerException(500, "boom").toErrorKind())
    }

    @Test
    fun `unknown throwable maps to Unknown`() {
        assertEquals(ErrorKind.Unknown, IllegalStateException("?").toErrorKind())
    }
}
