package me.modmuss50.mpp.networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object DefaultHttpImpl {
    val defaultJson = Json { ignoreUnknownKeys = true }

    val defaultAgent =
        "modmuss50/mod-publish-plugin/${DefaultHttpImpl::class.java.`package`.implementationVersion}"

    val defaultClient = client(java.time.Duration.ofSeconds(30))

    val defaultExceptionFactory: HttpExceptionFactory = { response ->
        val body = response.body<String>()

        val message =
            buildString {
                append("Request failed (status: ${response.status.value}, url: ${response.call.request.url})")
                if (body.isNotBlank()) {
                    append(" message: $body")
                }
            }

        HttpException(
            statusCode = response.status.value,
            url = response.call.request.url,
            body = body,
            headers =
                response.headers
                    .entries()
                    .associate { it.key to it.value },
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

    fun client(timeout: java.time.Duration) =
        HttpClient(CIO) {
            engine {
                requestTimeout = timeout.toMillis()
                endpoint {
                    connectTimeout = timeout.toMillis()
                }
            }
        }

    inline fun <reified T> jsonErrorFactory(crossinline messageExtractor: (T) -> String): HttpExceptionFactory =
        { response ->

            val body = response.body<String>()
            val json = Json { ignoreUnknownKeys = true }

            val baseMessage =
                try {
                    val parsed = json.decodeFromString<T>(body)
                    messageExtractor(parsed)
                } catch (_: SerializationException) {
                    body.ifBlank { "Unknown error" }
                }

            val uri = response.call.request.url

            val message = "${response.status.value} $uri: $baseMessage"

            HttpException(
                statusCode = response.status.value,
                url = uri,
                body = body,
                headers =
                    response.headers
                        .entries()
                        .associate { it.key to it.value },
                message = message,
            )
        }
}
