package me.modmuss50.mpp.test.misc

import me.modmuss50.mpp.MinecraftApi
import me.modmuss50.mpp.test.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class MinecraftApiTest {
    private lateinit var minecraftApi: MinecraftApi
    private lateinit var server: MockWebServer<MockMinecraftApi>

    @BeforeTest
    fun setup() {
        minecraftApi = MinecraftApi()
        server = MockWebServer(MockMinecraftApi())
    }

    @AfterTest
    fun cleanup() {
        server.close()
    }

    @Test
    fun getVersions() {
        val versions = minecraftApi.getVersionsInRange("1.19.4", "1.20.1")
        assertContains(versions, "1.19.4")
        assertContains(versions, "1.20")
        assertContains(versions, "1.20.1")
        assertFalse(versions.contains("1.20-rc1"))
    }

    @Test
    fun getVersionsSnapshots() {
        val versions = minecraftApi.getVersionsInRange("1.19.4", "1.20.1", true)
        assertContains(versions, "1.19.4")
        assertContains(versions, "1.20")
        assertContains(versions, "1.20.1")
        assertContains(versions, "1.20-rc1")
    }

    @Test
    fun getVersionsLatest() {
        val versions = minecraftApi.getVersionsInRange("1.19.4", "latest")
        assertContains(versions, "1.19.4")
        assertContains(versions, "1.20")
        assertContains(versions, "1.20.2")
        assertFalse(versions.contains("23w44a"))
    }

    @Test
    fun getVersionsLatestSnapshot() {
        val versions = minecraftApi.getVersionsInRange("1.19.4", "latest", true)
        assertContains(versions, "1.19.4")
        assertContains(versions, "1.20")
        assertContains(versions, "1.20.2")
        assertContains(versions, "23w44a")
    }

    @Test
    fun getSingleVersionFromRange() {
        val versions = minecraftApi.getVersionsInRange("1.19.4", "1.19.4", true)
        assert(versions.size == 1)
        assertContains(versions, "1.19.4")
    }
}
