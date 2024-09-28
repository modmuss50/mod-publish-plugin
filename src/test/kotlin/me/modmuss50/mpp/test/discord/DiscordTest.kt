package me.modmuss50.mpp.test.discord

import me.modmuss50.mpp.platforms.discord.DiscordAPI
import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import me.modmuss50.mpp.test.curseforge.MockCurseforgeApi
import me.modmuss50.mpp.test.github.MockGithubApi
import me.modmuss50.mpp.test.modrinth.MockModrinthApi
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiscordTest : IntegrationTest {
    @Test
    fun announceWebhook() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi(), MockModrinthApi(), MockGithubApi())))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        projectSlug = "test-mod"
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    modrinth {
                        accessToken = "123"
                        projectId = "12345678"                        
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    github {
                        accessToken = "123"
                        repository = "test/example"
                        commitish = "main"
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        var embeds = discordApi.requests.first().embeds!!
        embeds = embeds.sortedBy { it.url }

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertEquals(3, embeds.size)
        assertEquals("https://curseforge.com/minecraft/mc-mods/test-mod/files/20402", embeds[0].url)
        assertEquals("https://github.com", embeds[1].url)
        assertEquals("https://modrinth.com/mod/12345678/version/hFdJG9fY", embeds[2].url)
    }

    @Test
    fun announceWebhookSpecificPlatforms() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi(), MockModrinthApi(), MockGithubApi())))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        projectSlug = "test-mod"
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    modrinth {
                        accessToken = "123"
                        projectId = "12345678"                        
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    github {
                        accessToken = "123"
                        repository = "test/example"
                        commitish = "main"
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                        setPlatforms(platforms.get("curseforge"), platforms.get("github"))
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        var embeds = discordApi.requests.first().embeds!!
        embeds = embeds.sortedBy { it.url }

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertEquals(2, embeds.size)
        assertEquals("https://curseforge.com/minecraft/mc-mods/test-mod/files/20402", embeds[0].url)
        assertEquals("https://github.com", embeds[1].url)
    }

    @Test
    fun announceWebhookTitle() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi(), MockModrinthApi(), MockGithubApi())))

        val result = gradleTest()
            .buildScript(
                """
                val fabricJar = tasks.register("fabricJar", Jar::class.java) {
                    archiveClassifier = "fabric"
                }
                val forgeJar = tasks.register("forgeJar", Jar::class.java) {
                    archiveClassifier = "forge"
                }

                publishMods {
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                
                    // Common options that can be re-used between diffrent curseforge tasks
                    val options = curseforgeOptions {
                        accessToken = "123"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    curseforge("curseforgeFabric") {
                        from(options)
                        file = fabricJar.flatMap { it.archiveFile }
                        projectId = "123456"
                        modLoaders.add("fabric")
                        requires {
                            slug = "fabric-api"
                        }
                        announcementTitle = "Download for Fabric"
                        projectSlug = "fabric"
                    }
                    
                    curseforge("curseforgeForge") {
                        from(options)
                        file = forgeJar.flatMap { it.archiveFile }
                        projectId = "789123"
                        modLoaders.add("forge")
                        announcementTitle = "Download for Forge"
                        projectSlug = "forge"
                    }
                    
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        var embeds = discordApi.requests.first().embeds!!
        embeds = embeds.sortedBy { it.url }

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertEquals(2, embeds.size)
        assertEquals("Download for Fabric", embeds[0].title)
        assertEquals("Download for Forge", embeds[1].title)
    }

    @Test
    fun announceMoreThan10PlatformsEmbed() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi())))

        gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    // Create 25 dummy platforms
                    for (i in 1..25) {
                        curseforge("curseforge" + i) {
                            accessToken = "123"
                            projectId = "123456"
                            projectSlug = "test-mod-" + i
                            apiEndpoint = "${server.endpoint}"
                        }
                    }

                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        val requests = discordApi.requests

        assertEquals(3, requests.size)
        assertEquals(10, requests[0].embeds!!.size)
        assertEquals(10, requests[1].embeds!!.size)
        assertEquals(5, requests[2].embeds!!.size)

        val distinctUrls = requests.flatMap { it.embeds!! }.distinctBy { it.url }.size
        assertEquals(25, distinctUrls)

        assertNotNull(requests[0].content)
        assertNull(requests[1].content)
        assertNull(requests[2].content)
    }

    @Test
    fun announceMoreThan25PlatformsButton() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi())))

        gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    // Create 25 dummy platforms
                    for (i in 1..60) {
                        curseforge("curseforge" + i) {
                            accessToken = "123"
                            projectId = "123456"
                            projectSlug = "test-mod-" + i
                            apiEndpoint = "${server.endpoint}"
                        }
                    }

                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                        style {
                            link = BUTTON
                        }
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        val requests = discordApi.requests

        assertEquals(3, requests.size)
        assertNotNull(requests.first().components)
        assertEquals(5, requests.first().components!!.size)

        for (component in requests.first().components!!) {
            assertTrue(component is DiscordAPI.ActionRow)
            assertNotNull(component.components)
            assertEquals(5, component.components!!.size)
        }

        val distinctUrls = requests.flatMap { it.components!!.flatMap { (it as DiscordAPI.ActionRow).components!! as List<DiscordAPI.ButtonComponent> } }.distinctBy { it.url }.size
        assertEquals(60, distinctUrls)

        assertNotNull(requests[0].content)
        assertNull(requests[1].content)
        assertNull(requests[2].content)
    }

    @Test
    fun announceWebhookDryRun() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi)))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    dryRun = true
                    
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        projectSlug = "test-mod"
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    modrinth {
                        accessToken = "123"
                        projectId = "12345678"                        
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    github {
                        accessToken = "123"
                        repository = "test/example"
                        commitish = "main"
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                        dryRunWebhookUrl = "${server.endpoint}/api/webhooks/dryrun/def"
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        var embeds = discordApi.requests.first().embeds!!
        embeds = embeds.sortedBy { it.url }

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertEquals(3, embeds.size)
        assertEquals(1, discordApi.requestedKeys.size)
        assertContains(discordApi.requestedKeys.first(), "dryrun")
        assertEquals(3, embeds.distinctBy { it.url }.size)
    }

    @Test
    fun announceChildProjects() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi(), MockModrinthApi())))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    changelog = "# Changelog\n-123\n-epic feature"
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                        setPlatformsAllFrom(*project.subprojects.toTypedArray())
                    }
                }
                """.trimIndent(),
            )
            .subProject(
                "child1",
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        projectSlug = "test-mod"
                        apiEndpoint = "${server.endpoint}"
                    }
                }
                """.trimIndent(),
            )
            .subProject(
                "child2",
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    modrinth {
                        accessToken = "123"
                        projectId = "12345678"                        
                        apiEndpoint = "${server.endpoint}"
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        var embeds = discordApi.requests.first().embeds!!
        embeds = embeds.sortedBy { it.url }

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertEquals(2, embeds.size)
        assertEquals("https://curseforge.com/minecraft/mc-mods/test-mod/files/20402", embeds[0].url)
        assertEquals("https://modrinth.com/mod/12345678/version/hFdJG9fY", embeds[1].url)
    }

    @Test
    fun announceComponents() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi(), MockModrinthApi(), MockGithubApi())))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        projectSlug = "test-mod"
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    modrinth {
                        accessToken = "123"
                        projectId = "12345678"                        
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    github {
                        accessToken = "123"
                        repository = "test/example"
                        commitish = "main"
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                        style {
                            link = BUTTON
                        }
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        val first = discordApi.requests.first()
        var components = (first.components!! as List<DiscordAPI.ActionRow>).first().components!! as List<DiscordAPI.ButtonComponent>
        components = components.sortedBy { it.url }

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertNull(first.embeds)
        assertEquals(3, components.size)
        assertEquals("https://curseforge.com/minecraft/mc-mods/test-mod/files/20402", components[0].url)
        assertEquals("https://github.com", components[1].url)
        assertEquals("https://modrinth.com/mod/12345678/version/hFdJG9fY", components[2].url)
    }

    @Test
    fun announceInline() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi(), MockModrinthApi(), MockGithubApi())))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        projectSlug = "test-mod"
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    modrinth {
                        accessToken = "123"
                        projectId = "12345678"                        
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    github {
                        accessToken = "123"
                        repository = "test/example"
                        commitish = "main"
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                        style {
                            link = INLINE
                        }
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        val first = discordApi.requests.first()

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertNull(first.embeds)
        assertNull(first.components)
        assertNotNull(first.content)

        val links = first.content!!.split("\n").takeLast(3).sortedBy { it }
        assertEquals("[Download from CurseForge](https://curseforge.com/minecraft/mc-mods/test-mod/files/20402)", links[0])
        assertEquals("[Download from GitHub](https://github.com)", links[1])
        assertEquals("[Download from Modrinth](https://modrinth.com/mod/12345678/version/hFdJG9fY)", links[2])
    }

    @Test
    fun announceModern() {
        val discordApi = MockDiscordApi()
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(discordApi, MockCurseforgeApi(), MockModrinthApi(), MockGithubApi())))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "# Changelog\n-123\n-epic feature"
                    version = "1.0.0"
                    type = BETA
                    
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        projectSlug = "test-mod"
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    modrinth {
                        accessToken = "123"
                        projectId = "12345678"                        
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    github {
                        accessToken = "123"
                        repository = "test/example"
                        commitish = "main"
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                        style {
                            look = MODERN
                        }
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        var embeds = discordApi.requests.first().embeds!!
        embeds = embeds.sortedBy { it.url }

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertEquals(4, embeds.size)
        assertNull(embeds[0].url)
        assertEquals("https://curseforge.com/minecraft/mc-mods/test-mod/files/20402", embeds[1].url)
        assertEquals("https://github.com", embeds[2].url)
        assertEquals("https://modrinth.com/mod/12345678/version/hFdJG9fY", embeds[3].url)
    }
}
