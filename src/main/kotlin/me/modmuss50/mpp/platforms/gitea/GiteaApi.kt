package me.modmuss50.mpp.platforms.gitea

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.HttpUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File

class GiteaApi(private val accessToken: String, private val baseUrl: String, private val repository: String) {
    private val httpUtils = HttpUtils(
        exceptionFactory = GiteaHttpExceptionFactory(),
    )

    @Serializable
    data class Release(
        val id: Long,
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("upload_url")
        val uploadUrl: String,
    )

    // Some of the below are nullable, but we don't need their nullability here.
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

    fun createRelease(metadata: CreateRelease): Release {
        val body = Json.encodeToString(metadata).toRequestBody()
        return httpUtils.post("$baseUrl/repos/$repository/releases", body, headers)
    }

    fun getRelease(id: Long): Release {
        return httpUtils.get("$baseUrl/repos/$repository/releases/$id", headers)
    }

    fun uploadAsset(release: Release, name: String, file: File) {
        val mediaType = "application/java-archive".toMediaTypeOrNull()

        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("attachment", name, file.asRequestBody(mediaType))

        return httpUtils.post(release.uploadUrl, bodyBuilder.build(), headers)
    }

    fun publishRelease(release: Release) {
        val body = """
            {
            "draft": false
            }
        """.trimIndent().toRequestBody()
        return httpUtils.patch("$baseUrl/repos/$repository/releases/${release.id}", body, headers)
    }

    private class GiteaHttpExceptionFactory : HttpUtils.HttpExceptionFactory {
        val json = Json { ignoreUnknownKeys = true }

        override fun createException(response: Response): HttpUtils.HttpException {
            return try {
                val errorResponse = json.decodeFromString<ErrorResponse>(response.body!!.string())
                HttpUtils.HttpException(response, errorResponse.message)
            } catch (e: SerializationException) {
                HttpUtils.HttpException(response, "Unknown error")
            }
        }
    }
}
