package me.modmuss50.mpp.platforms.curseforge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.HttpUtils
import me.modmuss50.mpp.PlatformDependency
import me.modmuss50.mpp.PublishOptions
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.nio.file.Path
import kotlin.io.path.name

// https://support.curseforge.com/en/support/solutions/articles/9000197321-curseforge-upload-api
class CurseforgeApi(private val accessToken: String, private val baseUrl: String) {
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
            fun valueOf(type: PublishOptions.ReleaseType): ReleaseType {
                return when (type) {
                    PublishOptions.ReleaseType.STABLE -> RELEASE
                    PublishOptions.ReleaseType.BETA -> BETA
                    PublishOptions.ReleaseType.ALPHA -> ALPHA
                }
            }
        }
    }

    @Serializable
    data class UploadFileMetadata(
        val changelog: String, // Can be HTML or markdown if changelogType is set.
        val changelogType: String? = null, // Optional: defaults to text
        val displayName: String? = null, // Optional: A friendly display name used on the site if provided.
        val parentFileID: Int? = null, // Optional: The parent file of this file.
        val gameVersions: List<Int>, // A list of supported game versions, see the Game Versions API for details. Not supported if parentFileID is provided.
        val releaseType: ReleaseType,
        val relations: UploadFileRelations,
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
            fun valueOf(type: PlatformDependency.DependencyType): RelationType {
                return when (type) {
                    PlatformDependency.DependencyType.REQUIRED -> REQUIRED_DEPENDENCY
                    PlatformDependency.DependencyType.OPTIONAL -> OPTIONAL_DEPENDENCY
                    PlatformDependency.DependencyType.INCOMPATIBLE -> INCOMPATIBLE
                    PlatformDependency.DependencyType.EMBEDDED -> EMBEDDED_LIBRARY
                }
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

    private val headers: Map<String, String>
        get() = mapOf("X-Api-Token" to accessToken)

    fun getGameVersions(): List<GameVersion> {
        return HttpUtils.get("$baseUrl/api/game/versions", headers)
    }

    fun uploadFile(projectId: String, path: Path, uploadMetadata: UploadFileMetadata): UploadFileResponse {
        val mediaType = "application/java-archive".toMediaTypeOrNull()
        val fileBody = path.toFile().asRequestBody(mediaType)
        val metadataJson = Json.encodeToString(uploadMetadata)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", path.name, fileBody)
            .addFormDataPart("metadata", metadataJson)
            .build()

        return HttpUtils.post("$baseUrl/api/projects/$projectId/upload-file", requestBody, headers)
    }
}
