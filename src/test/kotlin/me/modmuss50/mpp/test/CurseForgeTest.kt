package me.modmuss50.mpp.test

import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CurseForgeTest : IntegrationTest {
    @Test
    fun uploadCurseForge() {
        val server = MockWebServer(MockCurseForgeApi())

        val result = gradleTest()
            .buildScript(
                """
                import me.modmuss50.mpp.PublishOptions
                import me.modmuss50.mpp.platforms.curseforge.CurseForge

                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile } // TODO is this really the best way?
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = PublishOptions.ReleaseType.BETA
                    modLoaders.add("fabric")

                    curseForge {
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
            .run("publishCurseForge")
        server.close()

        val metadata = server.api.lastMetadata!!

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseForge")!!.outcome)
        assertEquals(metadata.changelog, "Hello!")
        assertContains(metadata.gameVersions, 9990) // 1.20.1
        assertContains(metadata.gameVersions, 7499) // Fabric
        assertEquals(metadata.relations.projects[0].slug, "fabric-api")
    }
}
