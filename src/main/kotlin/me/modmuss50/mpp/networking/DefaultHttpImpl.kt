package me.modmuss50.mpp.networking

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.net.http.HttpClient
import java.time.Duration

object DefaultHttpImpl {
    val defaultJson = Json { ignoreUnknownKeys = true }
    val defaultAgent = "modmuss50/mod-publish-plugin/${DefaultHttpImpl::class.java.`package`.implementationVersion}"
    val defaultClient = client(Duration.ofSeconds(30))

    val defaultExceptionFactory: HttpExceptionFactory = { response ->
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

    val defaultProfile =
        HttpContext(
            client = defaultClient,
            json = defaultJson,
            userAgent = defaultAgent,
            exceptionFactory = defaultExceptionFactory,
        )

    val defaultConfig = HttpConfig(defaultProfile)

    fun client(timeout: Duration) =
        HttpClient
            .newBuilder()
            .connectTimeout(timeout)
            .build()

    inline fun <reified T> jsonErrorFactory(crossinline messageExtractor: (T) -> String): HttpExceptionFactory =
        { response ->
            val body = response.body().orEmpty()
            val json = Json { ignoreUnknownKeys = true }

            val message =
                try {
                    val parsed = json.decodeFromString<T>(body)
                    messageExtractor(parsed)
                } catch (_: SerializationException) {
                    body.ifBlank { "Unknown error" }
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
