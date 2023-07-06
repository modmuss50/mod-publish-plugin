package me.modmuss50.mpp.platforms.curseforge

import kotlinx.serialization.Serializable
import me.modmuss50.mpp.HttpUtils

// https://support.curseforge.com/en/support/solutions/articles/9000197321-curseforge-upload-api
class CurseForgeApi(val accessToken: String, val baseUrl: String = "https://minecraft.curseforge.com") {
    @Serializable
    data class GameDependency(
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
    data class UploadFileMetadata(
        val changelog: String, // Can be HTML or markdown if changelogType is set.
        val changelogType: String?, // Optional: defaults to text
        val displayName: String?, // Optional: A friendly display name used on the site if provided.
        val parentFileID: Int?, // Optional: The parent file of this file.
        val gameVersions: List<Int>, // A list of supported game versions, see the Game Versions API for details. Not supported if parentFileID is provided.
        val releaseType: String, // One of "alpha", "beta", "release".
    )

    @Serializable
    data class UploadFileRelations(
        val projects: List<ProjectFileRelation>,
    )

    @Serializable
    data class ProjectFileRelation(
        val slug: String, // Slug of related plugin.
        val type: String, // One of: "embeddedLibrary", "incompatible", "optionalDependency", "requiredDependency", "tool"
    )

    @Serializable
    data class UploadFileResponse(
        val id: Int,
    )

    fun getGameDependencies(): List<GameDependency> {
        return HttpUtils.get("/api/game/dependencies")
    }

    fun getGameVersions(): List<GameVersion> {
        return HttpUtils.get("/api/game/versions")
    }

    fun uploadFile(projectId: String): UploadFileResponse {
        return HttpUtils.post("/api/projects/$projectId/upload-file")
    }
}
