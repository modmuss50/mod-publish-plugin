package me.modmuss50.mpp.test

import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Enable this test to execute a dry run, this is more to test the discord integration. Create a options.json with all the tokens
 */
@Ignore
class DiscordIntegrationTest : IntegrationTest {
    @Test
    fun run() {
        //region Classic message body
        var result = gradleTest()
            .buildScript(
                createScript(""),
            )
            .run("publishMods")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)

        result = gradleTest()
            .buildScript(
                createScript(
                    """
                    style {
                        link = "BUTTON"
                    }
                    """.trimIndent(),
                ),
            )
            .run("publishMods")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)

        result = gradleTest()
            .buildScript(
                createScript(
                    """
                    style {
                        link = "INLINE"
                    }
                    """.trimIndent(),
                ),
            )
            .run("publishMods")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)
        //endregion

        //region Modern message body
        result = gradleTest()
            .buildScript(
                createScript(
                    """
                    style {
                        look = "MODERN"
                    }
                    """.trimIndent(),
                ),
            )
            .run("publishMods")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)

        result = gradleTest()
            .buildScript(
                createScript(
                    """
                    style {
                        look = "MODERN"
                        link = "BUTTON"
                    }
                    """.trimIndent(),
                ),
            )
            .run("publishMods")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)

        result = gradleTest()
            .buildScript(
                createScript(
                    """
                    style {
                        look = "MODERN"
                        link = "INLINE"
                    }
                    """.trimIndent(),
                ),
            )
            .run("publishMods")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)
        //endregion
    }

    private fun createScript(@Language("gradle") style: String): String {
        val options = Json.decodeFromString<ProductionOptions>(File("options.json").readText())

        @Language("gradle")
        val buildScript = """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "- Changelog line 1\n- Changelog line 2"
                    version = "1.0.0"
                    type = BETA
                    modLoaders.add("fabric")
                    displayName = "Test Upload"
                    dryRun = true
                
                    curseforge {
                        accessToken = "${options.curseforgeToken}"
                        projectId = "${options.curseforgeProject}"
                        projectSlug = "${options.curseforgeProjectSlug}"
                        minecraftVersions.add("1.20.1")
                        javaVersions.add(JavaVersion.VERSION_17)
                        clientRequired = true
                        serverRequired = true
                        
                        requires {
                            slug = "fabric-api"
                        }
                    }
                    
//                    github {
//                        accessToken = "${options.githubToken}"
//                        repository = "${options.githubRepo}"
//                        commitish = "main"
//                    }
                    
                    modrinth {
                        accessToken = "${options.modrinthToken}"
                        projectId = "${options.modrinthProject}"
                        minecraftVersions.add("1.20.1")
                        
                        requires {
                            id = "P7dR8mSH"
                        }
                    }
                    
                    discord {
                        username = "Great test mod"
                        avatarUrl = "https://placekitten.com/500/500"
                        content = changelog.map { "## A new version of my mod has been uploaded:\n" + it }
                        webhookUrl = "${options.discordWebhook}"
                        dryRunWebhookUrl = "${options.discordWebhook}"
                        $style
                    }
                }
                """

        return buildScript.trimIndent()
    }
}
