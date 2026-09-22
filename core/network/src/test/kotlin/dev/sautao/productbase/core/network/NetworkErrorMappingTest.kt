package dev.sautao.productbase.core.network

import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorMappingTest {
    @Test
    fun `unresolved host is reported as no connection`() {
        assertEquals(NetworkError.NoConnection, UnknownHostException("api.example.com").toNetworkError())
    }

    @Test
    fun `socket timeout is reported as timeout, not as no connection`() {
        // SocketTimeoutException is an IOException; if the branches were ordered the other way
        // round every timeout would be misreported. This test pins the order.
        assertEquals(NetworkError.Timeout, SocketTimeoutException().toNetworkError())
    }

    @Test
    fun `other IO failures fall back to no connection`() {
        assertEquals(NetworkError.NoConnection, ConnectException().toNetworkError())
        assertEquals(NetworkError.NoConnection, IOException("socket closed").toNetworkError())
    }

    @Test
    fun `http failures keep their status code`() {
        val exception = HttpException(
            Response.error<Unit>(404, "".toResponseBody("application/json".toMediaType())),
        )

        assertEquals(NetworkError.Http(404), exception.toNetworkError())
    }

    @Test
    fun `serialization failures are reported as malformed`() {
        assertEquals(NetworkError.Malformed, SerializationException("unexpected token").toNetworkError())
    }

    @Test
    fun `anything else is unexpected and keeps its cause`() {
        val cause = IllegalStateException("boom")

        assertEquals(NetworkError.Unexpected(cause), cause.toNetworkError())
    }
}
