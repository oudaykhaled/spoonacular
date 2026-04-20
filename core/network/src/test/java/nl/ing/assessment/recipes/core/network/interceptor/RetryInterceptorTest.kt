package nl.ing.assessment.recipes.core.network.interceptor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

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

    @Test(timeout = 10_000)
    fun `interrupted sleep propagates IOException and restores interrupt flag`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val captured = AtomicReference<Throwable?>(null)
        val call = client.newCall(
            Request.Builder().url(server.url("/")).build()
        )
        val thread = Thread {
            try {
                call.execute()
            } catch (t: Throwable) {
                captured.set(t)
            }
        }
        thread.start()

        awaitRequestCount(1)
        Thread.sleep(50)
        thread.interrupt()
        thread.join(5_000)

        val thrown = captured.get()
        assertTrue(
            "Expected IOException from interrupted sleep, got $thrown",
            thrown is IOException
        )
        assertEquals(1, server.requestCount)
    }

    @Test(timeout = 10_000)
    fun `canceled call short-circuits retry loop`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val captured = AtomicReference<Throwable?>(null)
        val call = client.newCall(
            Request.Builder().url(server.url("/")).build()
        )
        val thread = Thread {
            try {
                call.execute()
            } catch (t: Throwable) {
                captured.set(t)
            }
        }
        thread.start()

        awaitRequestCount(1)
        Thread.sleep(50)
        call.cancel()
        thread.join(5_000)

        val thrown = captured.get()
        assertTrue(
            "Expected IOException after cancel, got $thrown",
            thrown is IOException
        )
        assertEquals(1, server.requestCount)
    }

    private fun awaitRequestCount(target: Int, timeoutMillis: Long = 2_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (server.requestCount < target && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        assertEquals(
            "Expected server to receive $target request(s) within ${timeoutMillis}ms",
            target,
            server.requestCount
        )
    }
}
