package me.modmuss50.mpp.test.misc

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import me.modmuss50.mpp.test.curseforge.MockCurseforgeApi
import me.modmuss50.mpp.test.modrinth.MockModrinthApi
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MultiPlatformTest : IntegrationTest {
    @Test
    fun publishMultiplatform() {
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(MockCurseforgeApi(), MockModrinthApi())))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    changelog = "Changelog goes here"
                    version = "1.0.0"
                    type = STABLE
                
                    // CurseForge options used by both Fabric and Forge
                    val cfOptions = curseforgeOptions {
                        accessToken = "123"
                        projectId = "123456"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    // Modrinth options used by both Fabric and Forge
                    val mrOptions = modrinthOptions {
                        accessToken = "123"
                        projectId = "12345678"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    // Fabric specific options for CurseForge
                    curseforge("curseforgeFabric") {
                        from(cfOptions)
                        file(project(":fabric"))
                        modLoaders.add("fabric")
                        requires {
                            slug = "fabric-api"
                        }
                    }
                
                    // Forge specific options for CurseForge
                    curseforge("curseforgeForge") {
                        from(cfOptions)
                        file(project(":forge"))
                        modLoaders.add("forge")
                    }
                
                    // Fabric specific options for Modrinth
                    modrinth("modrinthFabric") {
                        from(mrOptions)
                        file(project(":fabric"))
                        modLoaders.add("fabric")
                        requires {
                            slug = "fabric-api"
                        }
                    }
                
                    // Forge specific options for Modrinth
                    modrinth("modrinthForge") {
                        from(mrOptions)
                        file(project(":forge"))
                        modLoaders.add("forge")
                    }
                }
                """.trimIndent(),
            )
            .subProject("fabric")
            .subProject("forge")
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":fabric:jar")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":forge:jar")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)
    }
}
