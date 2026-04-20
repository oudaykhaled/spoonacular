package nl.ing.assessment.recipes.core.logging

interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun logRecipeAction(recipeId: String, action: String, details: Map<String, Any?> = emptyMap())
    fun logSyncEvent(event: String, details: Map<String, Any?> = emptyMap())
    fun logNetworkRequest(method: String, url: String, durationMs: Long, statusCode: Int?)
    fun logError(tag: String, error: Throwable, context: Map<String, Any?> = emptyMap())
}
