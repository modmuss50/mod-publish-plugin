package me.modmuss50.mpp.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Enable this test to publish real files, to the real sites! Create a options.json with all the tokens
 */
@Ignore
class ProductionTest : IntegrationTest {
    @Test
    fun run() {
        val json = Json { ignoreUnknownKeys = true }
        val options = json.decodeFromString<ProductionOptions>(File("options.json").readText())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "- Changelog line 1\n- Changelog line 2"
                    version = "1.0.0"
                    type = BETA
                    modLoaders.add("fabric")
                    displayName = "Test Upload"
                
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
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)
    }
}

@Serializable
data class ProductionOptions(
    val curseforgeToken: String,
    val curseforgeProject: String,
    val curseforgeProjectSlug: String,
    val modrinthToken: String,
    val modrinthProject: String,
    val githubToken: String,
    val githubRepo: String,
    val discordWebhook: String,
)
