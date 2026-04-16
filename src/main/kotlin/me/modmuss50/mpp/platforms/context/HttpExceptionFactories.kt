package me.modmuss50.mpp.platforms.context

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

        HttpException(
            statusCode = response.statusCode(),
            uri = response.uri(),
            body = body,
            headers = response.headers().map(),
            response = response,
            message =
                buildString {
                    append("Request failed, status: ${response.statusCode()} ")
                    append("url: ${response.uri()}")
                    if (body.isNotBlank()) {
                        append(" message: $body")
                    }
                },
        )
    }

    val curseforge: HttpExceptionFactory = { response ->
        val json = Json { ignoreUnknownKeys = true }

        try {
            val errorResponse = json.decodeFromString<CurseforgeApi.ErrorResponse>(response.body())
            HttpException(
                statusCode = response.statusCode(),
                uri = response.uri(),
                body = response.body().orEmpty(),
                headers = response.headers().map(),
                response = response,
                message = errorResponse.errorMessage,
            )
        } catch (e: SerializationException) {
            HttpException(
                statusCode = response.statusCode(),
                uri = response.uri(),
                body = response.body().orEmpty(),
                headers = response.headers().map(),
                response = response,
                message = "Unknown error",
            )
        }
    }

    val modrinth: HttpExceptionFactory = { response ->
        val json = Json { ignoreUnknownKeys = true }

        try {
            val errorResponse = json.decodeFromString<ModrinthApi.ErrorResponse>(response.body())
            HttpException(
                statusCode = response.statusCode(),
                uri = response.uri(),
                body = response.body().orEmpty(),
                headers = response.headers().map(),
                response = response,
                message = errorResponse.description,
            )
        } catch (e: SerializationException) {
            HttpException(
                statusCode = response.statusCode(),
                uri = response.uri(),
                body = response.body().orEmpty(),
                headers = response.headers().map(),
                response = response,
                message = "Unknown error",
            )
        }
    }

    val gitea: HttpExceptionFactory = { response ->
        val json = Json { ignoreUnknownKeys = true }

        try {
            val errorResponse = json.decodeFromString<GiteaApi.ErrorResponse>(response.body())
            HttpException(
                statusCode = response.statusCode(),
                uri = response.uri(),
                body = response.body().orEmpty(),
                headers = response.headers().map(),
                response = response,
                message = errorResponse.message,
            )
        } catch (e: SerializationException) {
            HttpException(
                statusCode = response.statusCode(),
                uri = response.uri(),
                body = response.body().orEmpty(),
                headers = response.headers().map(),
                response = response,
                message = "Unknown error",
            )
        }
    }
}
