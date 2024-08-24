package me.modmuss50.mpp.platforms.modrinth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.HttpUtils
import me.modmuss50.mpp.PlatformDependency
import me.modmuss50.mpp.ReleaseType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.name

// https://docs.modrinth.com/api-spec/#tag/versions/operation/createVersion
class ModrinthApi(private val accessToken: String, private val baseUrl: String) {
    private val httpUtils = HttpUtils(
        exceptionFactory = ModrinthHttpExceptionFactory(),
        // Increase the timeout as Modrinth can be slow
        timeout = Duration.ofSeconds(60),
    )

    @Serializable
    enum class VersionType {
        @SerialName("alpha")
        ALPHA,

        @SerialName("beta")
        BETA,

        @SerialName("release")
        RELEASE,
        ;

        companion object {
            fun valueOf(type: ReleaseType): VersionType {
                return when (type) {
                    ReleaseType.STABLE -> RELEASE
                    ReleaseType.BETA -> BETA
                    ReleaseType.ALPHA -> ALPHA
                }
            }
        }
    }

    @Serializable
    data class CreateVersion(
        val name: String,
        @SerialName("version_number")
        val versionNumber: String,
        val changelog: String? = null,
        val dependencies: List<Dependency>,
        @SerialName("game_versions")
        val gameVersions: List<String>,
        @SerialName("version_type")
        val versionType: VersionType,
        val loaders: List<String>,
        val featured: Boolean,
        val status: String? = null,
        @SerialName("requested_status")
        val requestedStatus: String? = null,
        @SerialName("project_id")
        val projectId: String,
        @SerialName("file_parts")
        val fileParts: List<String>,
        @SerialName("primary_file")
        val primaryFile: String? = null,
    )

    @Serializable
    data class Dependency(
        @SerialName("version_id")
        val versionId: String? = null,
        @SerialName("project_id")
        val projectId: String? = null,
        @SerialName("file_name")
        val fileName: String? = null,
        @SerialName("dependency_type")
        val dependencyType: DependencyType,
    )

    @Serializable
    enum class DependencyType {
        @SerialName("required")
        REQUIRED,

        @SerialName("optional")
        OPTIONAL,

        @SerialName("incompatible")
        INCOMPATIBLE,

        @SerialName("embedded")
        EMBEDDED,
        ;

        companion object {
            fun valueOf(type: PlatformDependency.DependencyType): DependencyType {
                return when (type) {
                    PlatformDependency.DependencyType.REQUIRED -> REQUIRED
                    PlatformDependency.DependencyType.OPTIONAL -> OPTIONAL
                    PlatformDependency.DependencyType.INCOMPATIBLE -> INCOMPATIBLE
                    PlatformDependency.DependencyType.EMBEDDED -> EMBEDDED
                }
            }
        }
    }

    // There is a lot more to this response, however we dont need it.
    @Serializable
    data class CreateVersionResponse(
        val id: String,
        @SerialName("project_id")
        val projectId: String,
        @SerialName("author_id")
        val authorId: String,
    )

    @Serializable
    data class ProjectCheckResponse(
        val id: String,
    )

    // https://docs.modrinth.com/#tag/projects/operation/modifyProject
    @Serializable
    data class ModifyProject(
        val body: String,
    )

    @Serializable
    data class ErrorResponse(
        val error: String,
        val description: String,
    )

    private val headers: Map<String, String>
        get() = mapOf("Authorization" to accessToken)

    fun createVersion(metadata: CreateVersion, files: Map<String, Path>): CreateVersionResponse {
        val mediaType = "application/java-archive".toMediaTypeOrNull()
        val metadataJson = Json.encodeToString(metadata)

        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("data", metadataJson)

        for ((name, path) in files) {
            bodyBuilder.addFormDataPart(name, path.name, path.toFile().asRequestBody(mediaType))
        }

        return httpUtils.post("$baseUrl/version", bodyBuilder.build(), headers)
    }

    fun checkProject(projectSlug: String): ProjectCheckResponse {
        return httpUtils.get("$baseUrl/project/$projectSlug/check", headers)
    }

    fun modifyProject(projectSlug: String, modifyProject: ModifyProject) {
        val body = Json.encodeToString(modifyProject).toRequestBody()
        httpUtils.patch<String>("$baseUrl/project/$projectSlug", body, headers)
    }

    private class ModrinthHttpExceptionFactory : HttpUtils.HttpExceptionFactory {
        val json = Json { ignoreUnknownKeys = true }

        override fun createException(response: Response): HttpUtils.HttpException {
            return try {
                val errorResponse = json.decodeFromString<ErrorResponse>(response.body!!.string())
                HttpUtils.HttpException(response, errorResponse.description)
            } catch (e: SerializationException) {
                HttpUtils.HttpException(response, "Unknown error")
            }
        }
    }
}
