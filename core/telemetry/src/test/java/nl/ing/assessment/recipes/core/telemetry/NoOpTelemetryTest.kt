package nl.ing.assessment.recipes.core.telemetry

import org.junit.Assert.assertNotNull
import org.junit.Test

class NoOpTelemetryTest {

    @Test
    fun `NoOpEventTracker trackScreenView does not throw`() {
        val tracker: EventTracker = NoOpEventTracker()
        tracker.trackScreenView("SearchScreen")
        assertNotNull(tracker)
    }

    @Test
    fun `NoOpEventTracker trackEvent with empty params does not throw`() {
        val tracker: EventTracker = NoOpEventTracker()
        tracker.trackEvent("search_submitted")
        assertNotNull(tracker)
    }

    @Test
    fun `NoOpEventTracker trackEvent with params does not throw`() {
        val tracker: EventTracker = NoOpEventTracker()
        tracker.trackEvent("favorite_toggled", mapOf("recipeId" to 42, "value" to true))
        assertNotNull(tracker)
    }
}
