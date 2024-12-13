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
                version = "1.0.0"
                publishMods {
                    changelog = "Hello!"
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
                    requires {
                           id = "P7dR8mSH" // fabric-api
                    }
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
                        start = "1.13.1" // test WALL_OF_SHAME
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
        assertContains(gameVersions, "1.13.1")
        assertContains(gameVersions, "1.14.2-pre4")
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

    @Test
    fun updateProjectDescription() {
        val api = MockModrinthApi()
        val server = MockWebServer(api)

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
                    projectDescription = providers.fileContents(layout.projectDirectory.file("readme.md")).asText
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .file("readme.md", "Hello World")
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        assertEquals("Hello World", api.projectBody)
    }

    @Test
    fun uploadModrinthSlugDependency() {
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
                
                modrinth {
                    accessToken = "123"
                    minecraftVersions.add("1.20.1")
                    apiEndpoint = "${server.endpoint}"
                    projectId = "67896545"
                    modLoaders.add("fabric")
                    requires {
                        id = "P7dR8mSH"
                        version = "P7uGFii0"
                    }
                    requires {
                        slug = "fabric-api"
                        version = "0.92.1+1.20.1"
                    }
                }
            }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)

        var dependencies = mockModrinthApi.lastCreateVersion!!.dependencies.map { it.versionId }
        assertEquals(2, dependencies.size)
        assertContains(dependencies, "P7uGFii0")
        assertContains(dependencies, "ba99D9Qf")
    }
}
