package me.modmuss50.mpp.networking

/**
 * Configuration holder for HTTP-related infrastructure.
 *
 * This class wraps a base [HttpContext] and exposes a lazily-created [HttpApi]
 * instance derived from that context.
 *
 * @property context The base [HttpContext] containing shared HTTP configuration
 * such as the client, JSON serializer, user agent, and exception factory.
 */
data class HttpConfig(
    val context: HttpContext,
) {
    /**
     * Lazily initialized [HttpApi] instance built from the provided [context].
     */
    val httpApi by lazy {
        HttpApi(
            HttpContext(
                client = context.client,
                json = context.json,
                userAgent = context.userAgent,
                exceptionFactory = context.exceptionFactory,
            ),
        )
    }
}
