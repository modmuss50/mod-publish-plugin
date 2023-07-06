package me.modmuss50.mpp

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

object HttpUtils {
    val httpClient = OkHttpClient()

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
                throw IOException("Unexpected code $response")
            }

            return Json.decodeFromString<T>(response.body!!.string())
        }
    }
}
