package me.modmuss50.mpp.networking

data class HttpConfig(
    val context: HttpContext,
) {
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
