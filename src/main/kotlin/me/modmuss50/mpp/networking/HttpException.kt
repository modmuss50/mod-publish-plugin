package me.modmuss50.mpp.networking

import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url
import java.io.IOException

class HttpException(
    val statusCode: Int,
    val url: Url,
    val body: String,
    val headers: Map<String, List<String>>,
    message: String,
) : IOException(message)

typealias HttpExceptionFactory =
    suspend (HttpResponse) -> HttpException
