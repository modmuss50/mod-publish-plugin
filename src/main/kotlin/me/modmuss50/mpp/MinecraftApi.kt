package me.modmuss50.mpp

import kotlinx.serialization.Serializable
import me.modmuss50.mpp.networking.DefaultHttpImpl

class MinecraftApi(
    private val baseUrl: String = "https://piston-meta.mojang.com/",
) {
    private val httpUtils = DefaultHttpImpl.defaultConfig.httpApi

    @Serializable
    data class Version(
        val id: String,
        val type: String,
        val url: String,
        val time: String,
        val releaseTime: String,
    )

    @Serializable
    data class Latest(
        val release: String,
        val snapshot: String,
    )

    @Serializable
    data class LauncherMeta(
        val latest: Latest,
        val versions: List<Version>,
    )

    private val headers: Map<String, String>
        get() = mapOf()

    fun getLauncherMeta(): LauncherMeta = httpUtils.get("$baseUrl/mc/game/version_manifest_v2.json", headers)

    fun getVersions(): List<Version> = getLauncherMeta().versions

    fun getVersionsInRange(
        startId: String,
        endId: String,
        includeSnapshots: Boolean = false,
    ): List<String> {
        val launcherMeta = getLauncherMeta()
        val versions =
            launcherMeta.versions
                .filter { it.type == "release" || includeSnapshots }
                .map { it.id }
                .reversed()

        val startIndex = versions.indexOf(startId)
        val resolvedEndId =
            when (endId) {
                "latest" -> versions.last()
                "latestRelease" -> launcherMeta.latest.release
                else -> endId
            }
        val endIndex = versions.indexOf(resolvedEndId)

        if (startIndex == -1) throw IllegalArgumentException("Invalid start version $startId")
        if (endIndex == -1) throw IllegalArgumentException("Invalid end version $endId")
        if (startIndex > endIndex) throw IllegalArgumentException("Start version $startId must be before end version $endId")

        return versions.subList(startIndex, endIndex + 1)
    }
}
