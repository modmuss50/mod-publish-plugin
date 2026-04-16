package me.modmuss50.mpp.networking

import kotlinx.serialization.json.Json
import java.net.http.HttpClient

/**
 * Configuration container for internal HTTP operations.
 *
 * @property client The underlying [HttpClient] used to execute requests.
 * @property json The JSON serializer used for encoding and decoding data.
 * @property userAgent The default `User-Agent` header applied to requests.
 * @property exceptionFactory Factory used to convert HTTP responses into [HttpException]s.
 */
class HttpContext(
    val client: HttpClient,
    val json: Json,
    val userAgent: String,
    val exceptionFactory: HttpExceptionFactory,
)
