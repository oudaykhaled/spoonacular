package nl.ing.assessment.recipes.core.telemetry

interface EventTracker {
    fun trackScreenView(name: String)
    fun trackEvent(name: String, params: Map<String, Any?> = emptyMap())
}
