package me.modmuss50.mpp.networking

import java.io.IOException
import java.net.URI
import java.net.http.HttpResponse

class HttpException(
    val statusCode: Int,
    val uri: URI,
    val body: String,
    val headers: Map<String, List<String>>,
    val response: HttpResponse<String>?,
    message: String,
) : IOException(message)

typealias HttpExceptionFactory =
    (HttpResponse<String>) -> HttpException
