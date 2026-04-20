package nl.ing.assessment.recipes.core.logging

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class TimberLoggerTest {

    private val logEntries = mutableListOf<LogEntry>()
    private lateinit var logger: TimberLogger

    private val testTree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            logEntries.add(LogEntry(priority, tag, message, t))
        }
    }

    data class LogEntry(val priority: Int, val tag: String?, val message: String, val throwable: Throwable?)

    @Before
    fun setUp() {
        Timber.uprootAll()
        Timber.plant(testTree)
        logger = TimberLogger()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun `d logs debug message`() {
        logger.d("TestTag", "debug message")
        assertTrue(logEntries.any { it.tag == "TestTag" && it.message == "debug message" })
    }

    @Test
    fun `i logs info message`() {
        logger.i("InfoTag", "info message")
        assertTrue(logEntries.any { it.tag == "InfoTag" && it.message == "info message" })
    }

    @Test
    fun `w logs warning without throwable`() {
        logger.w("WarnTag", "warning message")
        assertTrue(logEntries.any { it.tag == "WarnTag" && it.message == "warning message" && it.throwable == null })
    }

    @Test
    fun `w logs warning with throwable`() {
        val ex = RuntimeException("oops")
        logger.w("WarnTag", "warning", ex)
        assertTrue(logEntries.any { it.tag == "WarnTag" && it.throwable == ex })
    }

    @Test
    fun `e logs error without throwable`() {
        logger.e("ErrTag", "error message")
        assertTrue(logEntries.any { it.tag == "ErrTag" && it.message == "error message" && it.throwable == null })
    }

    @Test
    fun `e logs error with throwable`() {
        val ex = RuntimeException("fail")
        logger.e("ErrTag", "error", ex)
        assertTrue(logEntries.any { it.tag == "ErrTag" && it.throwable == ex })
    }

    @Test
    fun `logRecipeAction logs with details`() {
        logger.logRecipeAction("recipe-1", "favorite", mapOf("source" to "list"))
        assertTrue(
            logEntries.any {
                it.tag == "RecipeAction" && it.message.contains("recipe-1") && it.message.contains("favorite")
            }
        )
    }

    @Test
    fun `logRecipeAction logs with empty details`() {
        logger.logRecipeAction("recipe-2", "view", emptyMap())
        assertTrue(logEntries.any { it.tag == "RecipeAction" && it.message.contains("recipe-2") })
    }

    @Test
    fun `logSyncEvent logs with details`() {
        logger.logSyncEvent("sync_started", mapOf("attempt" to 1))
        assertTrue(logEntries.any { it.tag == "Sync" && it.message.contains("sync_started") })
    }

    @Test
    fun `logSyncEvent logs with empty details`() {
        logger.logSyncEvent("sync_done", emptyMap())
        assertTrue(logEntries.any { it.tag == "Sync" && it.message.contains("sync_done") })
    }

    @Test
    fun `logNetworkRequest logs request details`() {
        logger.logNetworkRequest("GET", "https://example.com/recipes", 150L, 200)
        assertTrue(logEntries.any { it.tag == "Network" && it.message.contains("GET") && it.message.contains("200") })
    }

    @Test
    fun `logNetworkRequest logs null status code`() {
        logger.logNetworkRequest("POST", "https://example.com", 500L, null)
        assertTrue(logEntries.any { it.tag == "Network" && it.message.contains("failed") })
    }

    @Test
    fun `logError logs with context`() {
        val ex = RuntimeException("test error")
        logger.logError("ErrorTag", ex, mapOf("recipeId" to "123"))
        assertTrue(logEntries.any { it.tag == "ErrorTag" && it.throwable == ex && it.message.contains("recipeId") })
    }

    @Test
    fun `logError logs with empty context`() {
        val ex = RuntimeException("bare error")
        logger.logError("ErrorTag", ex, emptyMap())
        assertTrue(logEntries.any { it.tag == "ErrorTag" && it.throwable == ex })
    }

    @Test
    fun `Logger default parameters are invocable`() {
        logger.logRecipeAction("id", "action")
        logger.logSyncEvent("event")
        logger.logError("tag", RuntimeException("e"))
        assertTrue(logEntries.size >= 3)
    }
}
