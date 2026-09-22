package dev.sautao.productbase.core.network

import dev.sautao.productbase.core.network.di.NetworkModule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.http.GET
import kotlin.time.Duration.Companion.milliseconds

/**
 * Exercises the real stack — OkHttp, Retrofit, kotlinx-serialization and the error mapping —
 * against a local server, because the interesting failures live in how those pieces interact,
 * not in any one of them.
 */
class RetrofitStackTest {
    @get:Rule
    val server = MockWebServerRule()

    @Serializable
    data class SampleBody(val id: Long, val label: String)

    interface SampleApi {
        @GET("sample")
        suspend fun sample(): SampleBody
    }

    private fun api(config: NetworkConfig): SampleApi {
        val json = NetworkModule.provideJson()
        val client: OkHttpClient = NetworkModule.provideOkHttpClient(config)
        val retrofit: Retrofit = NetworkModule.provideRetrofit(config, client, json)
        return retrofit.create(SampleApi::class.java)
    }

    private fun config(
        readTimeout: kotlin.time.Duration = NetworkConfig(baseUrl = "").readTimeout,
    ) = NetworkConfig(baseUrl = server.server.url("/").toString(), readTimeout = readTimeout)

    @Test
    fun `parses a successful response`() = runTest {
        server.server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"id":7,"label":"seven"}""")
                .build(),
        )

        assertEquals(SampleBody(id = 7, label = "seven"), api(config()).sample())
    }

    @Test
    fun `ignores fields the client does not know about`() = runTest {
        server.server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"id":7,"label":"seven","addedByTheServerLater":true}""")
                .build(),
        )

        assertEquals(SampleBody(id = 7, label = "seven"), api(config()).sample())
    }

    @Test
    fun `maps a server error to Http with its status code`() = runTest {
        server.server.enqueue(MockResponse.Builder().code(503).body("").build())

        val error = runCatching { api(config()).sample() }.exceptionOrNull()?.toNetworkError()

        assertEquals(NetworkError.Http(503), error)
    }

    @Test
    fun `maps an unparseable body to Malformed`() = runTest {
        server.server.enqueue(
            MockResponse.Builder().code(200).body("""{"id":"not-a-number"}""").build(),
        )

        val error = runCatching { api(config()).sample() }.exceptionOrNull()?.toNetworkError()

        assertEquals(NetworkError.Malformed, error)
    }

    @Test
    fun `maps a stalled response to Timeout`() = runTest {
        server.server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"id":7,"label":"seven"}""")
                .bodyDelay(2, java.util.concurrent.TimeUnit.SECONDS)
                .build(),
        )

        val error = runCatching { api(config(readTimeout = 200.milliseconds)).sample() }
            .exceptionOrNull()
            ?.toNetworkError()

        assertEquals(NetworkError.Timeout, error)
    }
}
