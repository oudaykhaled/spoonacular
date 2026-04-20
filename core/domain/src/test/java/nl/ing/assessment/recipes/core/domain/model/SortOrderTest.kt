package nl.ing.assessment.recipes.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SortOrderTest {

    @Test
    fun `RELEVANCE maps to null`() {
        assertNull(SortOrder.RELEVANCE.toApiParam())
    }

    @Test
    fun `POPULARITY maps to popularity`() {
        assertEquals("popularity", SortOrder.POPULARITY.toApiParam())
    }

    @Test
    fun `HEALTHINESS maps to healthiness`() {
        assertEquals("healthiness", SortOrder.HEALTHINESS.toApiParam())
    }

    @Test
    fun `TIME maps to time`() {
        assertEquals("time", SortOrder.TIME.toApiParam())
    }

    @Test
    fun `PRICE maps to price`() {
        assertEquals("price", SortOrder.PRICE.toApiParam())
    }
}
