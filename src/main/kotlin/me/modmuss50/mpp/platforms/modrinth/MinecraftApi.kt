package me.modmuss50.mpp.platforms.modrinth

import kotlinx.serialization.Serializable
import me.modmuss50.mpp.HttpUtils

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
}
