package me.modmuss50.mpp.networking

import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * A small HTTP execution layer responsible for sending requests and decoding responses.
 *
 * This class wraps a configured [HttpContext] and provides a generic HTTP request method
 *
 * @property ctx The HTTP context containing the client, JSON, user agent, and exception factory.
 */
class HttpEngine(
    private val ctx: HttpContext,
) {
    /**
     * Executes an HTTP request and deserializes the response body into type [T].
     *
     * @param T The expected response type.
     * @param builder The [HttpRequest.Builder] used to construct the request.
     * @param headers Headers to include in the request.
     *
     * @return The deserialized response body as type [T].
     */
    internal inline fun <reified T> request(
        builder: HttpRequest.Builder,
        headers: Map<String, String> = emptyMap(),
    ): T {
        val request =
            builder
                .header("User-Agent", ctx.userAgent)
                .apply { headers.forEach(::header) }
                .build()

        val response =
            ctx.client
                .send(request, HttpResponse.BodyHandlers.ofString())
                .ensureSuccess(ctx.exceptionFactory)

        val body = response.body()

        // If the response body is blank, it is replaced with an empty JSON string in order to avoid deserialization failures.
        return ctx.json.decodeFromString<T>(body.ifBlank { "\"\"" })
    }
}

/**
 * Ensures that the HTTP response has a successful status code (2xx).
 *
 * @param factory A factory used to create an exception from the failed response.
 *
 * @return The original [HttpResponse] if the status code is successful.
 *
 * @throws Throwable created by [factory] if the response is not successful.
 */
private fun HttpResponse<String>.ensureSuccess(factory: HttpExceptionFactory): HttpResponse<String> =
    if (statusCode() in 200..299) {
        this
    } else {
        throw factory(this)
    }
