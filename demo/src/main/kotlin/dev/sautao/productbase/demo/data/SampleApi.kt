package dev.sautao.productbase.demo.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/**
 * Sample HTTP service.
 *
 * API interfaces and DTOs are product code — `core:network` deliberately ships none, so it never
 * has to know what any product's backend looks like.
 */
interface SampleApi {
    @GET("get")
    suspend fun echo(): EchoResponse
}

@Serializable
data class EchoResponse(val url: String, val origin: String? = null)
