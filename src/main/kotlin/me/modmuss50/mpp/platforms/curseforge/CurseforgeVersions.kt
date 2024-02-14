package me.modmuss50.mpp.platforms.curseforge

import org.gradle.api.JavaVersion

class CurseforgeVersions(
    private val versionTypes: List<CurseforgeApi.GameVersionType>,
    private val versions: List<CurseforgeApi.GameVersion>,
) {

    private fun getGameVersionTypes(name: String): List<Int> {
        val versions = if (name == "minecraft") {
            versionTypes.filter { it.slug.startsWith("minecraft") }
        } else {
            versionTypes.filter { it.slug == name }
        }.map { it.id }

        if (versions.isEmpty()) {
            throw IllegalStateException("Failed to find version type: $name")
        }

        return versions
    }

    private fun getVersion(name: String, type: String): Int {
        val versionTypes = getGameVersionTypes(type)
        val version = versions.find { versionTypes.contains(it.gameVersionTypeID) && it.name.equals(name, ignoreCase = true) } ?: throw IllegalStateException("Failed to find version: $name")
        return version.id
    }

    fun getMinecraftVersion(name: String): Int {
        return getVersion(name, "minecraft")
    }

    fun getModLoaderVersion(name: String): Int {
        return getVersion(name, "modloader")
    }

    fun getClientVersion(): Int {
        return getVersion("client", "environment")
    }

    fun getServerVersion(): Int {
        return getVersion("server", "environment")
    }

    fun getJavaVersion(version: JavaVersion): Int {
        return getVersion("Java ${version.ordinal + 1}", "java")
    }
}
