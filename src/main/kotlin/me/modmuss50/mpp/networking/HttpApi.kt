package me.modmuss50.mpp.networking

import java.net.URI
import java.net.http.HttpRequest

class HttpApi(
    private val ctx: HttpContext,
) {
    private val engine = HttpEngine(ctx)

    val json get() = ctx.json

    internal inline fun <reified T> get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): T =
        engine.request(
            HttpRequest.newBuilder().uri(URI.create(url)).GET(),
            headers,
        )

    internal inline fun <reified T> post(
        url: String,
        body: HttpRequest.BodyPublisher,
        headers: Map<String, String> = emptyMap(),
    ): T =
        engine.request(
            HttpRequest.newBuilder().uri(URI.create(url)).POST(body),
            headers,
        )

    internal inline fun <reified T> put(
        url: String,
        body: HttpRequest.BodyPublisher,
        headers: Map<String, String> = emptyMap(),
    ): T =
        engine.request(
            HttpRequest.newBuilder().uri(URI.create(url)).PUT(body),
            headers,
        )

    internal inline fun <reified T> patch(
        url: String,
        body: HttpRequest.BodyPublisher,
        headers: Map<String, String> = emptyMap(),
    ): T =
        engine.request(
            HttpRequest.newBuilder().uri(URI.create(url)).method("PATCH", body),
            headers,
        )
}
