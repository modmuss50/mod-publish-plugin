package me.modmuss50.mpp.test.discord

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordTest : IntegrationTest {
    @Test
    fun announceWebhook() {
        val server = MockWebServer(MockDiscordApi())
        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    changelog = "# Changelog\n-123\n-epic feature"
                    discord {
                        webhookUrl = "${server.endpoint}/api/webhooks/213/abc"
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":announceDiscord")!!.outcome)
    }
}
