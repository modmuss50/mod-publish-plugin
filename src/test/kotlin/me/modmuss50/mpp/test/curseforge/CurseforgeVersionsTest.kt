package me.modmuss50.mpp.test.curseforge

import kotlinx.serialization.json.Json
import me.modmuss50.mpp.platforms.curseforge.CurseforgeVersions
import org.gradle.api.JavaVersion
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import kotlin.test.assertEquals

class CurseforgeVersionsTest {
    val json = Json { ignoreUnknownKeys = true }

    @Test
    fun minecraftVersions() {
        val versions = createVersions()
        assertEquals(9990, versions.getMinecraftVersion("1.20.1"))
    }

    @Test
    fun modLoader() {
        val versions = createVersions()
        assertEquals(7499, versions.getModLoaderVersion("fabric"))
    }

    @Test
    fun client() {
        val versions = createVersions()
        assertEquals(9638, versions.getClientVersion())
    }

    @Test
    fun server() {
        val versions = createVersions()
        assertEquals(9639, versions.getServerVersion())
    }

    @Test
    fun javaVersions() {
        val versions = createVersions()
        assertEquals(4458, versions.getJavaVersion(JavaVersion.VERSION_1_8))
        assertEquals(8320, versions.getJavaVersion(JavaVersion.VERSION_11))
        assertEquals(8326, versions.getJavaVersion(JavaVersion.VERSION_17))
    }

    private fun createVersions(): CurseforgeVersions {
        val versionTypes = readResource("curseforge_version_types.json")
        val versions = readResource("curseforge_versions.json")
        return CurseforgeVersions(json.decodeFromString(versionTypes), json.decodeFromString(versions))
    }

    private fun readResource(path: String): String {
        this::class.java.classLoader!!.getResourceAsStream(path).use { inputStream ->
            BufferedReader(inputStream!!.reader()).use { reader ->
                return reader.readText()
            }
        }
    }
}
