package me.modmuss50.mpp

import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object HttpUtils {
    val httpClient: HttpClient = HttpClient.newHttpClient()

    inline fun <reified T> get(url: String): T {
        return request(
            HttpRequest.newBuilder()
                .uri(URI.create(url)),
        )
    }

    inline fun <reified T> post(url: String): T {
        TODO("Write a multipart/form-data impl")
        return request(
            HttpRequest.newBuilder()
                .uri(URI.create(url)),
        )
    }

    inline fun <reified T> request(requestBuilder: HttpRequest.Builder): T {
        val request = requestBuilder.build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return Json.decodeFromString<T>(response.body())
    }
}
