package me.modmuss50.mpp.networking

import kotlinx.serialization.json.Json
import java.net.http.HttpClient

class HttpContext(
    val client: HttpClient,
    val json: Json,
    val userAgent: String,
    val exceptionFactory: HttpExceptionFactory,
)
