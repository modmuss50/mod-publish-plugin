package me.modmuss50.mpp

import kotlinx.serialization.Serializable

class MinecraftApi(private val baseUrl: String = "https://piston-meta.mojang.com/") {
    private val httpUtils = HttpUtils()

    @Serializable
    data class Version(
        val id: String,
        val type: String,
        val url: String,
        val time: String,
        val releaseTime: String,
    )

    @Serializable
    data class LauncherMeta(
        val versions: List<Version>,
    )

    private val headers: Map<String, String>
        get() = mapOf()

    fun getVersions(): List<Version> {
        return httpUtils.get<LauncherMeta>("$baseUrl/mc/game/version_manifest_v2.json", headers).versions
    }

    fun getVersionsInRange(startId: String, endId: String, includeSnapshots: Boolean = false): List<String> {
        val versions = getVersions()
            .filter { it.type == "release" || includeSnapshots }
            .map { it.id }
            .reversed()

        val startIndex = versions.indexOf(startId)
        val endIndex = if (endId == "latest") versions.size - 1 else versions.indexOf(endId)

        if (startIndex == -1) throw IllegalArgumentException("Invalid start version $startId")
        if (endIndex == -1) throw IllegalArgumentException("Invalid end version $endId")
        if (startIndex > endIndex) throw IllegalArgumentException("Start version $startId must be before end version $endId")
        if (startIndex == endIndex) throw IllegalArgumentException("Start version $startId cannot be the same as end version $endId")

        return versions.subList(startIndex, endIndex + 1)
    }
}
