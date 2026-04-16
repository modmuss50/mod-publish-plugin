package me.modmuss50.mpp.networking

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
) : IOException(message)

/**
 * A factory function used to convert an [HttpResponse] into an [HttpException].
 *
 * This allows callers to customize how HTTP errors are represented, including
 * shaping error messages or extracting additional metadata from the response.
 */
typealias HttpExceptionFactory =
    (HttpResponse<String>) -> HttpException
