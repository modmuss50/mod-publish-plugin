package me.modmuss50.mpp.platforms.gitlab

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import me.modmuss50.mpp.HttpUtils
import me.modmuss50.mpp.MultipartBodyBuilder
import java.io.File
import java.net.http.HttpRequest

class GitlabApi(
    private val accessToken: String,
    private val apiEndpoint: String = "https://gitlab.com/api/v4",
) {
    private val httpUtils = HttpUtils()

    @Serializable
    data class Release(
        @SerialName("tag_name")
        val tagName: String,
        val name: String,
        val description: String,
        val assets: Assets? = null,
    )

    @Serializable
    data class Assets(
        val links: List<AssetLink> = emptyList(),
    )

    @Serializable
    data class AssetLink(
        val name: String,
        val url: String,
        @SerialName("link_type")
        val linkType: String,
    )

    @Serializable
    data class CreateReleaseRequest(
        val name: String,
        @SerialName("tag_name")
        val tagName: String,
        val description: String,
        val ref: String,
    )

    @Serializable
    data class UpdateReleaseRequest(
        val name: String? = null,
        val description: String? = null,
    )

    @Serializable
    data class UploadResponse(
        val id: Long,
        val alt: String,
        val url: String,
    )

    private val headers: Map<String, String>
        get() = mapOf("PRIVATE-TOKEN" to accessToken)

    fun createRelease(
        projectId: Long,
        request: CreateReleaseRequest,
    ): Release {
        val url = "$apiEndpoint/projects/$projectId/releases"
        val body = HttpRequest.BodyPublishers.ofString(httpUtils.json.encodeToString(request))
        val headersWithContentType = headers + ("Content-Type" to "application/json")
        return httpUtils.post(url, body, headersWithContentType)
    }

    fun getRelease(
        projectId: Long,
        tagName: String,
    ): Release {
        val url = "$apiEndpoint/projects/$projectId/releases/$tagName"
        return httpUtils.get(url, headers)
    }

    fun updateRelease(
        projectId: Long,
        tagName: String,
        request: UpdateReleaseRequest,
    ): Release {
        val url = "$apiEndpoint/projects/$projectId/releases/$tagName"
        val body = HttpRequest.BodyPublishers.ofString(httpUtils.json.encodeToString(request))
        val headersWithContentType = headers + ("Content-Type" to "application/json")
        return httpUtils.put(url, body, headersWithContentType)
    }

    fun uploadAsset(
        projectId: Long,
        file: File,
    ): AssetLink {
        val url = "$apiEndpoint/projects/$projectId/uploads"
        val builder = MultipartBodyBuilder().addFormDataPart("file", file.name, file)
        val bodyPublisher = builder.build()
        val headersWithContentType = headers + ("Content-Type" to builder.getContentType())
        val response: UploadResponse = httpUtils.post(url, bodyPublisher, headersWithContentType)

        return AssetLink(
            name = file.name,
            url = response.url,
            linkType = "other",
        )
    }

    /**
     * Needed to update existing releases through the GitLab tag system.
     */
    fun addAssetToRelease(
        projectId: Long,
        tagName: String,
        asset: AssetLink,
    ) {
        val release = getRelease(projectId, tagName)
        val newDescription = buildString {
            append(release.description)
            if (!release.description.endsWith("\n")) append("\n")
            append("[${asset.name}](${asset.url})")
        }
        updateRelease(projectId, tagName, UpdateReleaseRequest(description = newDescription))
    }
}
