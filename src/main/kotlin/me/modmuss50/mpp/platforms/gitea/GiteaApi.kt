package me.modmuss50.mpp.platforms.gitea

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.HttpUtils
import me.modmuss50.mpp.MultipartBodyBuilder
import java.io.File
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GiteaApi(private val accessToken: String, private val baseUrl: String, private val repository: String) {
    private val httpUtils = HttpUtils(
        exceptionFactory = GiteaHttpExceptionFactory(),
    )

    @Serializable
    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoGetRelease
    data class Release(
        val id: Long,
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("upload_url")
        val uploadUrl: String,
    )

    // Some of the below are nullable, but we don't need their nullability here.
    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoCreateRelease
    @Serializable
    data class CreateRelease(
        val body: String? = null,
        val draft: Boolean,
        val name: String? = null,
        val prerelease: Boolean,
        @SerialName("tag_name")
        val tagName: String,
        val targetCommitish: String,
    )

    // Error responses are consistent between hooks.
    @Serializable
    data class ErrorResponse(
        val message: String,
        val url: String,
    )

    private val headers: Map<String, String>
        get() = mapOf(
            "Authorization" to "token $accessToken",
            "Content-Type" to "application/json",
        )

    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoCreateRelease
    fun createRelease(metadata: CreateRelease): Release {
        val body = HttpRequest.BodyPublishers.ofString(Json.encodeToString(metadata))
        return httpUtils.post("$baseUrl/repos/$repository/releases", body, headers)
    }

    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoGetRelease
    fun getRelease(id: Long): Release {
        return httpUtils.get("$baseUrl/repos/$repository/releases/$id", headers)
    }

    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoCreateReleaseAttachment
    fun uploadAsset(release: Release, file: File) {
        val bodyBuilder = MultipartBodyBuilder()
            .addFormDataPart("attachment", file.name, file, "application/java-archive")

        val multipartHeaders = headers.toMutableMap()
        multipartHeaders["Content-Type"] = bodyBuilder.getContentType()

        return httpUtils.post(release.uploadUrl, bodyBuilder.build(), multipartHeaders)
    }

    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoEditRelease
    fun publishRelease(release: Release) {
        val body = HttpRequest.BodyPublishers.ofString(
            """
            {
            "draft": false
            }
            """.trimIndent(),
        )
        return httpUtils.patch("$baseUrl/repos/$repository/releases/${release.id}", body, headers)
    }

    // Error responses are consistent between hooks.
    private class GiteaHttpExceptionFactory : HttpUtils.HttpExceptionFactory {
        val json = Json { ignoreUnknownKeys = true }

        override fun createException(response: HttpResponse<String>): HttpUtils.HttpException {
            return try {
                val errorResponse = json.decodeFromString<ErrorResponse>(response.body())
                HttpUtils.HttpException(response, errorResponse.message)
            } catch (e: SerializationException) {
                HttpUtils.HttpException(response, "Unknown error")
            }
        }
    }
}
