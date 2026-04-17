package me.modmuss50.mpp.platforms.curseforge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import me.modmuss50.mpp.networking.MultipartBodyBuilder
import me.modmuss50.mpp.PlatformDependency
import me.modmuss50.mpp.platforms.HttpClients
import java.nio.file.Path
import kotlin.io.path.name

// https://support.curseforge.com/en/support/solutions/articles/9000197321-curseforge-upload-api
class CurseforgeApi(
    private val accessToken: String,
    private val baseUrl: String,
) {
    private val httpUtils = HttpClients.curseforgeClient

    @Serializable
    data class GameVersionType(
        val id: Int,
        val name: String,
        val slug: String,
    )

    @Serializable
    data class GameVersion(
        val id: Int,
        val gameVersionTypeID: Int,
        val name: String,
        val slug: String,
    )

    @Serializable
    enum class ReleaseType {
        @SerialName("alpha")
        ALPHA,

        @SerialName("beta")
        BETA,

        @SerialName("release")
        RELEASE,
        ;

        companion object {
            fun valueOf(type: me.modmuss50.mpp.ReleaseType): ReleaseType =
                when (type) {
                    me.modmuss50.mpp.ReleaseType.STABLE -> RELEASE
                    me.modmuss50.mpp.ReleaseType.BETA -> BETA
                    me.modmuss50.mpp.ReleaseType.ALPHA -> ALPHA
                }
        }
    }

    @Serializable
    enum class ChangelogType {
        @SerialName("text")
        TEXT,

        @SerialName("html")
        HTML,

        @SerialName("markdown")
        MARKDOWN,
        ;

        companion object {
            @JvmStatic
            fun of(value: String): ChangelogType {
                val upper = value.uppercase()
                try {
                    return ChangelogType.valueOf(upper)
                } catch (e: java.lang.IllegalArgumentException) {
                    throw java.lang.IllegalArgumentException("Invalid changelog type: $upper. Must be one of: TEXT, HTML, MARKDOWN")
                }
            }
        }
    }

    @Serializable
    data class UploadFileMetadata(
        val changelog: String, // Can be HTML or Markdown if changelogType is set.
        val changelogType: ChangelogType? = null, // Optional: defaults to text
        val displayName: String? = null, // Optional: A friendly display name used on the site if provided.
        val parentFileID: Int? = null, // Optional: The parent file of this file.
        val gameVersions: List<Int>?, // A list of supported game versions, see the Game Versions API for details. Not supported if parentFileID is provided.
        val releaseType: ReleaseType,
        val relations: UploadFileRelations? = null,
    )

    @Serializable
    data class UploadFileRelations(
        val projects: List<ProjectFileRelation>,
    )

    enum class RelationType {
        @SerialName("embeddedLibrary")
        EMBEDDED_LIBRARY,

        @SerialName("incompatible")
        INCOMPATIBLE,

        @SerialName("optionalDependency")
        OPTIONAL_DEPENDENCY,

        @SerialName("requiredDependency")
        REQUIRED_DEPENDENCY,

        @SerialName("tool")
        TOOL,
        ;

        companion object {
            fun valueOf(type: PlatformDependency.DependencyType): RelationType =
                when (type) {
                    PlatformDependency.DependencyType.REQUIRED -> REQUIRED_DEPENDENCY
                    PlatformDependency.DependencyType.OPTIONAL -> OPTIONAL_DEPENDENCY
                    PlatformDependency.DependencyType.INCOMPATIBLE -> INCOMPATIBLE
                    PlatformDependency.DependencyType.EMBEDDED -> EMBEDDED_LIBRARY
                }
        }
    }

    @Serializable
    data class ProjectFileRelation(
        val slug: String, // Slug of related plugin.
        val type: RelationType,
    )

    @Serializable
    data class UploadFileResponse(
        val id: Int,
    )

    @Serializable
    data class ErrorResponse(
        val errorCode: Int,
        val errorMessage: String,
    )

    private val headers: Map<String, String>
        get() = mapOf("X-Api-Token" to accessToken)

    fun getVersionTypes(): List<GameVersionType> =
        httpUtils.get(
            url = "$baseUrl/api/game/version-types",
            headers = headers,
        )

    fun getGameVersions(): List<GameVersion> =
        httpUtils.get(
            url = "$baseUrl/api/game/versions",
            headers = headers,
        )

    fun uploadFile(
        projectId: String,
        path: Path,
        uploadMetadata: UploadFileMetadata,
    ): UploadFileResponse {
        val metadataJson = httpUtils.json.encodeToString(uploadMetadata)

        val bodyBuilder =
            MultipartBodyBuilder()
                .addFormDataPart("file", path.name, path, "application/java-archive")
                .addFormDataPart("metadata", metadataJson)

        val multipartHeaders =
            headers.toMutableMap().apply {
                this["Content-Type"] = bodyBuilder.getContentType()
            }

        return httpUtils.post(
            url = "$baseUrl/api/projects/$projectId/upload-file",
            body = bodyBuilder.build(),
            headers = multipartHeaders,
        )
    }
}
