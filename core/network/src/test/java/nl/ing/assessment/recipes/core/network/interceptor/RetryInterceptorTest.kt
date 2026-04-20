package nl.ing.assessment.recipes.core.network.interceptor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful request does not retry`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val response = client.newCall(
            Request.Builder().url(server.url("/")).build()
        ).execute()

        assertEquals(200, response.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `retries on 503 twice then succeeds and returns 200`() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(200).setBody("recovered"))

        val response = client.newCall(
            Request.Builder().url(server.url("/")).build()
        ).execute()

        assertEquals(200, response.code)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `does not retry on 4xx responses`() {
        server.enqueue(MockResponse().setResponseCode(404))

        val response = client.newCall(
            Request.Builder().url(server.url("/")).build()
        ).execute()

        assertEquals(404, response.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `returns 5xx when retries are exhausted`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))

        val response = client.newCall(
            Request.Builder().url(server.url("/")).build()
        ).execute()

        assertEquals(500, response.code)
        assertEquals(3, server.requestCount)
    }

    @Test(expected = java.io.IOException::class)
    fun `wraps non-IOException as IOException after retries exhausted`() {
        val throwing = okhttp3.Interceptor { _ -> throw RuntimeException("Unexpected failure") }

        val clientWithThrowing = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
            .addInterceptor(throwing)
            .build()

        clientWithThrowing.newCall(
            Request.Builder().url(server.url("/")).build()
        ).execute()
    }
}
