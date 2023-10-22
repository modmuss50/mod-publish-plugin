package me.modmuss50.mpp.test.modrinth

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ModrinthTest : IntegrationTest {
    @Test
    fun uploadModrinth() {
        val server = MockWebServer(MockModrinthApi())

        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
            
                modrinth {
                    accessToken = "123"
                    projectId = "12345678"
                    minecraftVersions.add("1.20.1")
                    
                    requires {
                        id = "P7dR8mSH"
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
    }

    @Test
    fun uploadModrinthWithOptions() {
        val server = MockWebServer(MockModrinthApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                
                    // Common options that can be re-used between diffrent modrinth tasks
                    val modrinthOptions = modrinthOptions {
                        accessToken = "123"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    modrinth("modrinthFabric") {
                        from(modrinthOptions)
                        file = tasks.jar.flatMap { it.archiveFile }
                        projectId = "12345678"
                        modLoaders.add("fabric")
                        requires {
                           id = "P7dR8mSH" // fabric-api
                        }
                    }
                    
                    modrinth("modrinthForge") {
                        from(modrinthOptions)
                        file = tasks.jar.flatMap { it.archiveFile }
                        projectId = "67896545"
                        modLoaders.add("forge")
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinthFabric")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinthForge")!!.outcome)
    }

    @Test
    fun dryRunModrinth() {
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

                modrinth {
                    accessToken = providers.environmentVariable("TEST_TOKEN_THAT_DOES_NOT_EXISTS")
                    projectId = "12345678"
                    minecraftVersions.add("1.20.1")
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
    }

    @Test
    fun uploadModrinthNoDeps() {
        val server = MockWebServer(MockModrinthApi())

        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
            
                modrinth {
                    accessToken = "123"
                    projectId = "12345678"
                    minecraftVersions.add("1.20.1")

                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
    }

    @Test
    fun invalidId() {
        val server = MockWebServer(MockModrinthApi())

        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
            
                modrinth {
                    accessToken = "123"
                    projectId = "invalid-id"
                    minecraftVersions.add("1.20.1")

                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.FAILED, result.task(":publishModrinth")!!.outcome)
        result.output.contains("invalid-id is not a valid Modrinth ID")
    }

    @Test
    fun uploadModrinthSlugLookup() {
        val server = MockWebServer(MockModrinthApi())

        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
            
                modrinth {
                    accessToken = "123"
                    projectId = "12345678"
                    minecraftVersions.add("1.20.1")
                    
                    requires {
                        slug = "fabric-api"
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
    }

    @Test
    fun uploadModrinthMinecraftVersionRange() {
        val mockModrinthApi = MockModrinthApi()
        val server = MockWebServer(mockModrinthApi)

        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
            
                modrinth {
                    accessToken = "123"
                    projectId = "12345678"

                    minecraftVersionRange {
                        start = "1.19.4"
                        end = "1.20.2"
                        includeSnapshots = true
                    }

                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        val gameVersions = mockModrinthApi.lastCreateVersion!!.gameVersions
        assertContains(gameVersions, "1.19.4")
        assertContains(gameVersions, "1.20-pre1")
        assertContains(gameVersions, "1.20.1")
        assertContains(gameVersions, "1.20.2")
    }

    @Test
    fun uploadModrinthMinecraftVersionRangeNoSnapshots() {
        val mockModrinthApi = MockModrinthApi()
        val server = MockWebServer(mockModrinthApi)

        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
            
                modrinth {
                    accessToken = "123"
                    projectId = "12345678"

                    minecraftVersionRange {
                        start = "1.19.4"
                        end = "1.20.2"
                    }

                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        val gameVersions = mockModrinthApi.lastCreateVersion!!.gameVersions
        assertContains(gameVersions, "1.19.4")
        assertFalse(gameVersions.contains("1.20-pre1"))
        assertContains(gameVersions, "1.20.1")
        assertContains(gameVersions, "1.20.2")
    }
}
