package me.modmuss50.mpp.networking

import kotlinx.serialization.json.Json
import java.net.http.HttpClient
import java.time.Duration

/**
 * Configuration container for internal HTTP operations.
 *
 * @property client The underlying [HttpClient] used to execute requests.
 * @property json The JSON serializer used for encoding and decoding data.
 * @property userAgent The default `User-Agent` header applied to requests.
 * @property exceptionFactory Factory used to convert HTTP responses into [HttpException]s.
 */
class RequestContext(
    val json: Json,
    val userAgent: String,
    val client: HttpClient,
    val exceptionFactory: HttpExceptionFactory,
) {
    /**
     * Container for default configuration values.
     */
    object Default {
        val json = Json { ignoreUnknownKeys = true }
        val userAgent = "modmuss50/mod-publish-plugin/${RequestContext::class.java.`package`.implementationVersion}"
        val client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build()
        val exceptionFactory: HttpExceptionFactory = { response ->
            val body = response.body().orEmpty()

            val message =
                buildString {
                    append("Request failed (status: ${response.statusCode()}, url: ${response.uri()})")
                    if (body.isNotBlank()) {
                        append(" message: $body")
                    }
                }

            HttpException(
                statusCode = response.statusCode(),
                uri = response.uri(),
                body = body,
                headers = response.headers().map(),
                response = response,
                message = message,
            )
        }
    }
}
