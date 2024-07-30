package me.modmuss50.mpp.test.misc

import me.modmuss50.mpp.MinecraftApi
import me.modmuss50.mpp.test.MockWebServer
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class MinecraftApiTest {
    @Test
    fun getVersions() {
        val server = MockWebServer(MockMinecraftApi())
        val api = MinecraftApi(server.endpoint)

        val versions = api.getVersionsInRange("1.19.4", "1.20.1")
        assertContains(versions, "1.19.4")
        assertContains(versions, "1.20")
        assertContains(versions, "1.20.1")
        assertFalse(versions.contains("1.20-rc1"))

        server.close()
    }

    @Test
    fun getVersionsSnapshots() {
        val server = MockWebServer(MockMinecraftApi())
        val api = MinecraftApi(server.endpoint)

        val versions = api.getVersionsInRange("1.19.4", "1.20.1", true)
        assertContains(versions, "1.19.4")
        assertContains(versions, "1.20")
        assertContains(versions, "1.20.1")
        assertContains(versions, "1.20-rc1")

        server.close()
    }

    @Test
    fun getVersionsLatest() {
        val server = MockWebServer(MockMinecraftApi())
        val api = MinecraftApi(server.endpoint)

        val versions = api.getVersionsInRange("1.19.4", "latest")
        assertContains(versions, "1.19.4")
        assertContains(versions, "1.20")
        assertContains(versions, "1.20.2")
        assertFalse(versions.contains("23w44a"))

        server.close()
    }

    @Test
    fun getVersionsLatestSnapshot() {
        val server = MockWebServer(MockMinecraftApi())
        val api = MinecraftApi(server.endpoint)

        val versions = api.getVersionsInRange("1.19.4", "latest", true)
        assertContains(versions, "1.19.4")
        assertContains(versions, "1.20")
        assertContains(versions, "1.20.2")
        assertContains(versions, "23w44a")

        server.close()
    }

    @Test
    fun getSingleVersionFromRange() {
        val server = MockWebServer(MockMinecraftApi())
        val api = MinecraftApi(server.endpoint)

        val versions = api.getVersionsInRange("1.19.4", "1.19.4", true)
        assert(versions.size == 1)
        assertContains(versions, "1.19.4")

        server.close()
    }
}
