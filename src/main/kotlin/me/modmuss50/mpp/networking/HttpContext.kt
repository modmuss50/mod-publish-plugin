package me.modmuss50.mpp.networking

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

class HttpContext(
    val client: HttpClient,
    val json: Json,
    val userAgent: String,
    val exceptionFactory: HttpExceptionFactory,
)
