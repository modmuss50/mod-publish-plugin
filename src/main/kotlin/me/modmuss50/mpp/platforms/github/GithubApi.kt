package me.modmuss50.mpp.platforms.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import me.modmuss50.mpp.HttpUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class GithubApi(private val accessToken: String, private val apiEndpoint: String = "https://api.github.com") {
    private val httpUtils = HttpUtils()

    @Serializable
    data class Repository(
        @SerialName("full_name")
        val fullName: String,
    )

    @Serializable
    data class Release(
        val id: Long,
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("upload_url")
        val uploadUrl: String,
    )

    @Serializable
    data class CreateReleaseRequest(
        @SerialName("tag_name")
        val tagName: String,
        @SerialName("target_commitish")
        val targetCommitish: String,
        val name: String,
        val body: String,
        val draft: Boolean,
        val prerelease: Boolean,
    )

    @Serializable
    data class UpdateReleaseRequest(
        val draft: Boolean,
    )

    @Serializable
    class Asset

    private val headers: Map<String, String>
        get() = mapOf(
            "Authorization" to "token $accessToken",
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
        )

    // https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#get-a-repository
    fun getRepository(repository: String): Repository {
        val url = "$apiEndpoint/repos/$repository"
        return httpUtils.get(url, headers)
    }

    // https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28#create-a-release
    fun createRelease(repository: String, request: CreateReleaseRequest): Release {
        val url = "$apiEndpoint/repos/$repository/releases"
        val body = httpUtils.json.encodeToString(request).toRequestBody("application/json".toMediaType())
        return httpUtils.post(url, body, headers)
    }

    // https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28#get-a-release
    fun getRelease(repository: String, releaseId: Long): Release {
        val url = "$apiEndpoint/repos/$repository/releases/$releaseId"
        return httpUtils.get(url, headers)
    }

    // https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28#update-a-release
    fun updateRelease(repository: String, releaseId: Long, request: UpdateReleaseRequest): Release {
        val url = "$apiEndpoint/repos/$repository/releases/$releaseId"
        val body = httpUtils.json.encodeToString(request).toRequestBody("application/json".toMediaType())
        return httpUtils.patch(url, body, headers)
    }

    // https://docs.github.com/en/rest/releases/assets?apiVersion=2022-11-28#upload-a-release-asset
    fun uploadAsset(release: Release, file: File) {
        // Parse the upload URL template and replace {?name,label} with the actual query params
        val uploadUrl = release.uploadUrl.substringBefore("{")
        val url = "$uploadUrl?name=${file.name}"
        val body = file.asRequestBody("application/java-archive".toMediaType())
        httpUtils.post<Asset>(url, body, headers)
    }
}
