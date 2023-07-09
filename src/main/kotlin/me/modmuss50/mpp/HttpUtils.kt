package me.modmuss50.mpp

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.lang.RuntimeException

object HttpUtils {
    val httpClient = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }

    inline fun <reified T> get(url: String, headers: Map<String, String>): T {
        return request(
            Request.Builder()
                .url(url),
            headers,
        )
    }

    inline fun <reified T> post(url: String, body: RequestBody, headers: Map<String, String>): T {
        return request(
            Request.Builder()
                .url(url)
                .post(body),
            headers,
        )
    }

    inline fun <reified T> request(requestBuilder: Request.Builder, headers: Map<String, String>): T {
        for ((name, value) in headers) {
            requestBuilder.header(name, value)
        }

        val request = requestBuilder.build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw HttpException(response.code, response.message)
            }

            return json.decodeFromString<T>(response.body!!.string())
        }
    }

    /**
     * Retry server errors
     */
    fun <T> retry(maxRetries: Int, message: String, closure: () -> T): T {
        var exception: RuntimeException? = null
        var count = 0

        while (count < maxRetries) {
            try {
                return closure()
            } catch (e: HttpException) {
                if (e.code < 500 || e.code > 599) {
                    throw e
                }

                // Only retry 5xx server errors
                count++
                exception = exception ?: RuntimeException(message)
                exception.addSuppressed(e)
            }
        }

        throw exception!!
    }

    class HttpException(val code: Int, message: String) : IOException(message)
}
