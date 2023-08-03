package me.modmuss50.mpp.test.misc

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import me.modmuss50.mpp.test.curseforge.MockCurseforgeApi
import me.modmuss50.mpp.test.modrinth.MockModrinthApi
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class OptionsTest : IntegrationTest {
    @Test
    fun uploadWithPublishOptions() {
        val server = MockWebServer(MockWebServer.CombinedApi(listOf(MockModrinthApi(), MockCurseforgeApi())))

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                
                    val fabricOptions = publishOptions {
                        displayName = "Test Fabric"
                        modLoaders.add("fabric")
                    }
                    
                    val forgeOptions = publishOptions {
                        displayName = "Test Forge"
                        modLoaders.add("forge")
                    }
                    
                    val curseForgeOptions = curseforgeOptions {
                        accessToken = "123"
                        projectId = "123456"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                    
                    val modrinthOptions = modrinthOptions {
                        accessToken = "123"
                        projectId = "12345678"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    curseforge("curseforgeFabric") {
                        from(curseForgeOptions, fabricOptions)
                        requires {
                            slug = "fabric-api"
                        }
                    }
                
                    curseforge("curseforgeForge") {
                        from(curseForgeOptions, forgeOptions)
                    }
                    
                    modrinth("modrinthFabric") {
                        from(modrinthOptions, fabricOptions)
                        requires {
                            slug = "fabric-api"
                        }
                    }
                
                    modrinth("modrinthForge") {
                        from(modrinthOptions, forgeOptions)
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishMods")!!.outcome)
    }
}
