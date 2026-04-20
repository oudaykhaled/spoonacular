package nl.ing.assessment.recipes.core.logging

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimberLogger @Inject constructor() : Logger {

    init {
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    override fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Timber.tag(tag).w(throwable, message) else Timber.tag(tag).w(message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Timber.tag(tag).e(throwable, message) else Timber.tag(tag).e(message)
    }

    override fun logRecipeAction(recipeId: String, action: String, details: Map<String, Any?>) {
        val detailsStr = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Timber.tag("RecipeAction").d("[$recipeId] $action ${if (detailsStr.isNotEmpty()) "| $detailsStr" else ""}")
    }

    override fun logSyncEvent(event: String, details: Map<String, Any?>) {
        val detailsStr = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Timber.tag("Sync").i("$event ${if (detailsStr.isNotEmpty()) "| $detailsStr" else ""}")
    }

    override fun logNetworkRequest(method: String, url: String, durationMs: Long, statusCode: Int?) {
        Timber.tag("Network").d("$method $url -> ${statusCode ?: "failed"} (${durationMs}ms)")
    }

    override fun logError(tag: String, error: Throwable, context: Map<String, Any?>) {
        val contextStr = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Timber.tag(tag).e(error, "Error occurred ${if (contextStr.isNotEmpty()) "| $contextStr" else ""}")
    }
}
