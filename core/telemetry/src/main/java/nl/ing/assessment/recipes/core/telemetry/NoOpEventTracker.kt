package nl.ing.assessment.recipes.core.telemetry

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpEventTracker @Inject constructor() : EventTracker {
    override fun trackScreenView(name: String) {}
    override fun trackEvent(name: String, params: Map<String, Any?>) {}
}
