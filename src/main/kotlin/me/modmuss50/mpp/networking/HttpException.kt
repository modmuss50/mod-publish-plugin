package me.modmuss50.mpp.networking

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.http.HttpResponse

/**
 * Exception representing a non-successful HTTP response.
 *
 * @property statusCode The HTTP status code returned by the server.
 * @property uri The URI that was requested.
 * @property body The response body as a string.
 * @property headers The response headers.
 * @property response The original [HttpResponse], if available.
 * @param message A human-readable error message describing the failure.
 */
class HttpException(
    val statusCode: Int,
    val uri: URI,
    val body: String,
    val headers: Map<String, List<String>>,
    val response: HttpResponse<String>?,
    message: String,
) : IOException(message) {
    companion object {
        /**
         * Helper designed to create a JSON-based [HttpExceptionFactory].
         *
         * @param json A [Json] instance.
         * @param messageExtractor Describes how messages should be extracted from parsed JSON.
         *
         * @return An [HttpException].
         */
        inline fun <reified T> jsonErrorFactory(json: Json, crossinline messageExtractor: (T) -> String): HttpExceptionFactory =
            { response ->
                val body = response.body().orEmpty()

                val baseMessage =
                    try {
                        val parsed = json.decodeFromString<T>(body)
                        messageExtractor(parsed)
                    } catch (_: SerializationException) {
                        body.ifBlank { "Unknown error" }
                    }

                val message = "${response.statusCode()} ${response.uri()}: $baseMessage"

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

/**
 * A factory function used to convert an [HttpResponse] into an [HttpException].
 *
 * This allows callers to customize how HTTP errors are represented, including
 * shaping error messages or extracting additional metadata from the response.
 */
typealias HttpExceptionFactory =
            (HttpResponse<String>) -> HttpException