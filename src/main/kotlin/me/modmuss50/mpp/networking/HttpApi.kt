package me.modmuss50.mpp.networking

import java.net.URI
import java.net.http.HttpRequest

/**
 * HTTP API wrapper providing convenient request methods.
 *
 * This object builds on top of [HttpEngine] to expose common HTTP operations
 * such as GET, POST, PUT, and PATCH with minimal boilerplate.
 *
 */
object HttpApi {
    /**
     * Executes an HTTP GET request and deserializes the response body into type [T].
     *
     * @param T The expected response type.
     * @param url The target URL.
     * @param headers Additional headers to include in the request.
     *
     * @return The deserialized response body as type [T].
     */
    internal inline fun <reified T> RequestContext.get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): T =
        HttpEngine.request(
            this,
            HttpRequest.newBuilder().uri(URI.create(url)).GET(),
            headers,
        )

    /**
     * Executes an HTTP POST request with a request body and deserializes the response into type [T].
     *
     * @param T The expected response type.
     * @param url The target URL.
     * @param body The request body publisher.
     * @param headers Additional headers to include in the request.
     *
     * @return The deserialized response body as type [T].
     */
    internal inline fun <reified T> RequestContext.post(
        url: String,
        body: HttpRequest.BodyPublisher,
        headers: Map<String, String> = emptyMap(),
    ): T =
        HttpEngine.request(
            this,
            HttpRequest.newBuilder().uri(URI.create(url)).POST(body),
            headers,
        )

    /**
     * Executes an HTTP PUT request with a request body and deserializes the response into type [T].
     *
     * @param T The expected response type.
     * @param url The target URL.
     * @param body The request body publisher.
     * @param headers Additional headers to include in the request.
     *
     * @return The deserialized response body as type [T].
     */
    internal inline fun <reified T> RequestContext.put(
        url: String,
        body: HttpRequest.BodyPublisher,
        headers: Map<String, String> = emptyMap(),
    ): T =
        HttpEngine.request(
            this,
            HttpRequest.newBuilder().uri(URI.create(url)).PUT(body),
            headers,
        )

    /**
     * Executes an HTTP PATCH request with a request body and deserializes the response into type [T].
     *
     * Note: PATCH is not directly supported as a dedicated method in [HttpRequest.Builder],
     * so it is invoked via [HttpRequest.Builder.method].
     *
     * @param T The expected response type.
     * @param url The target URL.
     * @param body The request body publisher.
     * @param headers Additional headers to include in the request.
     *
     * @return The deserialized response body as type [T].
     */
    internal inline fun <reified T> RequestContext.patch(
        url: String,
        body: HttpRequest.BodyPublisher,
        headers: Map<String, String> = emptyMap(),
    ): T =
        HttpEngine.request(
            this,
            HttpRequest.newBuilder().uri(URI.create(url)).method("PATCH", body),
            headers,
        )
}
