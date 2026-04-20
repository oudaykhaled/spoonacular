package nl.ing.assessment.recipes.core.network.interceptor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommonHeadersInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(CommonHeadersInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `adds Accept header`() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("application/json", recorded.getHeader("Accept"))
    }

    @Test
    fun `adds User-Agent header with IngRecipes prefix`() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute()

        val recorded = server.takeRequest()
        val ua = recorded.getHeader("User-Agent")
        assertNotNull(ua)
        assertTrue("Expected User-Agent to start with IngRecipes/ but was '$ua'", ua!!.startsWith("IngRecipes/"))
    }
}
