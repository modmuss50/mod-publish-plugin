package me.modmuss50.mpp.platforms

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.networking.HttpException
import me.modmuss50.mpp.networking.HttpExceptionFactory
import me.modmuss50.mpp.platforms.curseforge.CurseforgeApi
import me.modmuss50.mpp.platforms.gitea.GiteaApi
import me.modmuss50.mpp.platforms.modrinth.ModrinthApi

object HttpExceptionFactories {
    val default: HttpExceptionFactory = { response ->
        val body = response.body().orEmpty()

        val message =
            buildString {
                append("Request failed (status: ${response.statusCode()}, url: ${response.uri()})")
                if (body.isNotBlank()) {
                    append(" message: $body")
                }
            }

        HttpException(
            statusCode = response.statusCode(),
            uri = response.uri(),
            body = body,
            headers = response.headers().map(),
            response = response,
            message = message,
        )
    }

    val curseforge =
        jsonErrorFactory<CurseforgeApi.ErrorResponse> {
            it.errorMessage
        }

    val modrinth =
        jsonErrorFactory<ModrinthApi.ErrorResponse> {
            it.description
        }

    val gitea =
        jsonErrorFactory<GiteaApi.ErrorResponse> {
            it.message
        }

    private inline fun <reified T> jsonErrorFactory(crossinline messageExtractor: (T) -> String): HttpExceptionFactory =
        { response ->
            val body = response.body().orEmpty()
            val json = Json { ignoreUnknownKeys = true }

            val message =
                try {
                    val parsed = json.decodeFromString<T>(body)
                    messageExtractor(parsed)
                } catch (_: SerializationException) {
                    body.ifBlank { "Unknown error" }
                }

            HttpException(
                statusCode = response.statusCode(),
                uri = response.uri(),
                body = body,
                headers = response.headers().map(),
                response = response,
                message = message,
            )
        }
}
