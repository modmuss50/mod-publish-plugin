package me.modmuss50.mpp.networking

import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod

class HttpApi(
    private val ctx: HttpContext,
) {
    private val engine = HttpEngine(ctx)

    val json get() = ctx.json

    internal suspend inline fun <reified T> get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): T =
        engine.request<T>(
            url = url,
            headers = headers,
        ) {
            method = HttpMethod.Get
        }

    internal suspend inline fun <reified T> post(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
    ): T =
        engine.request<T>(
            url = url,
            headers = headers,
        ) {
            method = HttpMethod.Post

            body?.let {
                setBody(it)
            }
        }

    internal suspend inline fun <reified T> put(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
    ): T =
        engine.request<T>(
            url = url,
            headers = headers,
        ) {
            method = HttpMethod.Put

            body?.let {
                setBody(it)
            }
        }

    internal suspend inline fun <reified T> patch(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
    ): T =
        engine.request<T>(
            url = url,
            headers = headers,
        ) {
            method = HttpMethod.Patch

            body?.let {
                setBody(it)
            }
        }
}
