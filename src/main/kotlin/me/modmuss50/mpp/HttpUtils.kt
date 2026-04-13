package me.modmuss50.mpp

import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class HttpUtils(val exceptionFactory: HttpExceptionFactory = DefaultHttpExceptionFactory(), timeout: Duration = Duration.ofSeconds(30)) {
    val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .build()
    val json = Json { ignoreUnknownKeys = true }

    val userAgent = "modmuss50/mod-publish-plugin/${HttpUtils::class.java.`package`.implementationVersion}"

    inline fun <reified T> get(url: String, headers: Map<String, String>): T {
        return request(
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET(),
            headers,
        )
    }

    inline fun <reified T> post(url: String, body: HttpRequest.BodyPublisher, headers: Map<String, String>): T {
        return request(
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(body),
            headers,
        )
    }

    inline fun <reified T> patch(url: String, body: HttpRequest.BodyPublisher, headers: Map<String, String>): T {
        return request(
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("PATCH", body),
            headers,
        )
    }

    // Added for GitLab's REST API
    inline fun <reified T> put(url: String, body: HttpRequest.BodyPublisher, headers: Map<String, String>): T {
        return request(
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(body),
            headers,
        )
    }

    inline fun <reified T> request(requestBuilder: HttpRequest.Builder, headers: Map<String, String>): T {
        requestBuilder.header("User-Agent", userAgent)

        for ((name, value) in headers) {
            requestBuilder.header(name, value)
        }

        val request = requestBuilder.build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw exceptionFactory.createException(response)
        }

        var body = response.body()

        if (body.isBlank()) {
            // A bit of a hack, but handle empty body's as an empty string.
            body = "\"\""
        }

        return json.decodeFromString<T>(body)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HttpUtils::class.java)

        /**
         * Retry server errors
         */
        fun <T> retry(maxRetries: Int, message: String, closure: () -> T): T {
            var exception: RuntimeException? = null
            var count = 0

            while (count < maxRetries) {
                try {
                    return closure()
                } catch (e: Exception) {
                    count++
                    exception = exception ?: RuntimeException("$message after $maxRetries attempts with error: ${e.message}")
                    exception.addSuppressed(e)
                }
            }

            LOGGER.error("$message failed after $maxRetries retries", exception)
            throw exception!!
        }
    }

    interface HttpExceptionFactory {
        fun createException(response: HttpResponse<String>): HttpException
    }

    private class DefaultHttpExceptionFactory : HttpExceptionFactory {
        override fun createException(response: HttpResponse<String>): HttpException {
            return HttpException(response, response.body() ?: "No response body")
        }
    }

    class HttpException(val response: HttpResponse<String>, message: String) : IOException("Request failed, status: ${response.statusCode()} message: $message url: ${response.uri()}")
}
