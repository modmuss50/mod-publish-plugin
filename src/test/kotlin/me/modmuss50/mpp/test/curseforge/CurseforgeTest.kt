package me.modmuss50.mpp.test.curseforge

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CurseforgeTest : IntegrationTest {
    @Test
    fun uploadCurseForge() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest()
            .buildScript(
                """
                import me.modmuss50.mpp.PublishOptions
                
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                    modLoaders.add("fabric")
                
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        minecraftVersions.add("1.20.1")
                        
                        requires {
                            slug = "fabric-api"
                        }
                        
                        apiEndpoint = "${server.endpoint}"
                    }
                }
                """.trimIndent(),
            )
            .run("publishCurseforge")
        server.close()

        val metadata = server.api.lastMetadata!!

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforge")!!.outcome)
        assertEquals(metadata.changelog, "Hello!")
        assertContains(metadata.gameVersions, 9990) // 1.20.1
        assertContains(metadata.gameVersions, 7499) // Fabric
        assertEquals(metadata.relations.projects[0].slug, "fabric-api")
    }
}
