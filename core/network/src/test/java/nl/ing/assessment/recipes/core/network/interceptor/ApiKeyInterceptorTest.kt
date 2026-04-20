package nl.ing.assessment.recipes.core.network.interceptor

import nl.ing.assessment.recipes.core.network.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class ApiKeyInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `adds apiKey query parameter when key is non-empty and not already present`() {
        assumeTrue(
            "Skipping: SPOONACULAR_API_KEY is empty in this build.",
            BuildConfig.SPOONACULAR_API_KEY.isNotEmpty()
        )
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(
            Request.Builder().url(server.url("/recipes/complexSearch?query=pasta")).build()
        ).execute()

        val recorded = server.takeRequest()
        val requestUrl = recorded.requestUrl
        assertNotNull(requestUrl)
        assertEquals(BuildConfig.SPOONACULAR_API_KEY, requestUrl!!.queryParameter("apiKey"))
        assertTrue(recorded.path!!.contains("apiKey="))
    }

    @Test
    fun `does not add apiKey when already present in url`() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(
            Request.Builder().url(server.url("/recipes?apiKey=existing")).build()
        ).execute()

        val recorded = server.takeRequest()
        val requestUrl = recorded.requestUrl!!
        assertEquals("existing", requestUrl.queryParameter("apiKey"))
        assertEquals(1, requestUrl.queryParameterValues("apiKey").size)
    }

    @Test
    fun `does not add apiKey when BuildConfig key is empty`() {
        assumeTrue(
            "Skipping: test only meaningful when SPOONACULAR_API_KEY is empty.",
            BuildConfig.SPOONACULAR_API_KEY.isEmpty()
        )
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(
            Request.Builder().url(server.url("/recipes?query=pasta")).build()
        ).execute()

        val recorded = server.takeRequest()
        val requestUrl = recorded.requestUrl!!
        org.junit.Assert.assertNull(requestUrl.queryParameter("apiKey"))
    }
}
