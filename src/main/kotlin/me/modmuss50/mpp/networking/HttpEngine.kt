package me.modmuss50.mpp.networking

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod

class HttpEngine(
    private val ctx: HttpContext,
) {
    internal suspend inline fun <reified T> request(
        url: String,
        headers: Map<String, String> = emptyMap(),
        block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response =
            ctx.client.request(url) {
                method = HttpMethod.Get

                header(HttpHeaders.UserAgent, ctx.userAgent)
                headers.forEach { (k, v) -> header(k, v) }

                block()
            }

        if (response.status.value !in 200..299) {
            throw ctx.exceptionFactory(response)
        }

        val body = response.bodyAsText()

        val safeBody = body.ifBlank { "\"\"" }

        return ctx.json.decodeFromString<T>(safeBody)
    }
}
