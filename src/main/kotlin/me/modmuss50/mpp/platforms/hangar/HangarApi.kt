package me.modmuss50.mpp.platforms.hangar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.networking.HttpApi.post
import me.modmuss50.mpp.networking.MultipartBodyBuilder
import me.modmuss50.mpp.networking.RequestContext
import org.gradle.api.Incubating
import java.io.File
import java.lang.IllegalArgumentException
import java.net.URLEncoder

/*
* https://github.com/HangarMC/hangar-publish-plugin/blob/master/plugin/src/main/java/io/papermc/hangarpublishplugin/internal/HangarVersion.java
*/
class HangarApi(
    private val apiKey: String,
    private val apiEndpoint: String = "https://hangar.papermc.io/api/v1/",
) {
    companion object {
        val httpContext = RequestContext(
            json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
            userAgent = RequestContext.Default.userAgent,
            client = RequestContext.Default.client,
            exceptionFactory = RequestContext.Default.exceptionFactory,
        )
    }

    @Serializable
    data class VersionUpload(
        val channel: String,
        val description: String?,
        val files: List<FileUpload>?,
        val platformDependencies: Map<String, List<String>>?,
        val pluginDependencies: Map<String, List<DependencyUpload>>?,
        val version: String,
    )

    @Serializable
    data class FileUpload(
        val externalUrl: String?,
        val platforms: List<HangarPlatformType>,
    )

    @Serializable
    data class DependencyUpload(
        val externalUrl: String?,
        val name: String?,
        val platform: HangarPlatformType,
        val projectId: Int?,
        val required: Boolean,
    )

    @Serializable
    data class VersionUploadResponse(
        val url: String,
    )

    @Incubating
    @Serializable
    enum class HangarPlatformType {
        PAPER,
        WATERFALL,
        VELOCITY,
        ;

        companion object {
            @JvmStatic
            fun of(value: String): HangarPlatformType {
                val upper = value.uppercase()
                try {
                    return valueOf(upper)
                } catch (_: IllegalArgumentException) {
                    throw IllegalArgumentException("Invalid platform type: $upper. Must be one of: PAPER, WATERFALL, VELOCITY")
                }
            }
        }
    }

    private val headers: Map<String, String>
        get() = mapOf("Authorization" to "Bearer $apiKey")

    fun publishVersion(
        projectSlug: String,
        upload: VersionUpload,
        files: List<File> = emptyList(),
    ): VersionUploadResponse {
        val url = "$apiEndpoint/projects/${encodeSlug(projectSlug)}/upload"

        val builder = MultipartBodyBuilder()
            .addFormDataPart(
                "versionUpload",
                httpContext.json.encodeToString(upload),
            )

        files.forEach { file ->
            builder.addFormDataPart(
                "files",
                file.name,
                file,
            )
        }

        return httpContext.post(
            url = url,
            body = builder.build(),
            headers = headers + ("Content-Type" to builder.getContentType()),
        )
    }

    fun encodeSlug(slug: String): String =
        slug.split("/")
            .joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8) }
}
