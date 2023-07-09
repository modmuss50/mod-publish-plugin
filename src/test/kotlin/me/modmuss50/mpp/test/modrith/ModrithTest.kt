package me.modmuss50.mpp.test.modrith

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class ModrithTest : IntegrationTest {
    @Test
    fun uploadModrith() {
        val server = MockWebServer(MockModrithApi())

        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
            
                modrith {
                    accessToken = "123"
                    projectId = "123456"
                    minecraftVersions.add("1.20.1")
                    
                    requires {
                        projectId = "P7dR8mSH"
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrith")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrith")!!.outcome)
    }

    @Test
    fun uploadModrithWithOptions() {
        val server = MockWebServer(MockModrithApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                
                    // Common options that can be re-used between diffrent modrith tasks
                    val modrithOptions = modrithOptions {
                        accessToken = "123"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    modrith("modrithFabric") {
                        from(modrithOptions)
                        file = tasks.jar.flatMap { it.archiveFile }
                        projectId = "123456"
                        modLoaders.add("fabric")
                        requires {
                           projectId = "P7dR8mSH" // fabric-api
                        }
                    }
                    
                    modrith("modrithForge") {
                        from(modrithOptions)
                        file = tasks.jar.flatMap { it.archiveFile }
                        projectId = "789123"
                        modLoaders.add("forge")
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrithFabric")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrithForge")!!.outcome)
    }

    @Test
    fun dryRunModrith() {
        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
                dryRun = true

                modrith {
                    accessToken = providers.environmentVariable("TEST_TOKEN_THAT_DOES_NOT_EXISTS")
                    projectId = "123456"
                    minecraftVersions.add("1.20.1")
                }
            }
                """.trimIndent(),
            )
            .run("publishModrith")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrith")!!.outcome)
    }
}
