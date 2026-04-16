package me.modmuss50.mpp.networking

import java.net.http.HttpResponse

fun HttpResponse<String>.ensureSuccess(factory: HttpExceptionFactory): HttpResponse<String> =
    if (statusCode() in 200..299) {
        this
    } else {
        throw factory(this)
    }
