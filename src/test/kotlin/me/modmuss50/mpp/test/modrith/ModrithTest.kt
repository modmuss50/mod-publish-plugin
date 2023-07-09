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
            import me.modmuss50.mpp.PublishOptions
            
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
}
