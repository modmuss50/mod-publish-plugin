package me.modmuss50.mpp.networking

import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HttpEngine(
    private val ctx: HttpContext,
) {
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

        return ctx.json.decodeFromString<T>(body.ifBlank { "\"\"" })
    }
}
