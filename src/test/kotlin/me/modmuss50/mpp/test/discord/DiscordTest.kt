package me.modmuss50.mpp.test.discord

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import me.modmuss50.mpp.test.curseforge.MockCurseforgeApi
import me.modmuss50.mpp.test.github.MockGithubApi
import me.modmuss50.mpp.test.modrinth.MockModrinthApi
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

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

        var embeds = discordApi.request!!.embeds!!
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

        var embeds = discordApi.request!!.embeds!!
        embeds = embeds.sortedBy { it.url }

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
        assertEquals(2, embeds.size)
        assertEquals("https://curseforge.com/minecraft/mc-mods/test-mod/files/20402", embeds[0].url)
        assertEquals("https://github.com", embeds[1].url)
    }
}
