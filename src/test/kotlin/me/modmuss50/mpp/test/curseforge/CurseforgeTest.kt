package me.modmuss50.mpp.test.curseforge

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CurseforgeTest : IntegrationTest {
    @Test
    fun uploadCurseforge() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                    modLoaders.add("fabric")
                    displayName = "Test Upload"
                
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        minecraftVersions.add("1.20.1")
                        
                        requires {
                            slug = "fabric-api"
                        }
                        
                        requires("mod-test", "mod-test-2")
                        
                        apiEndpoint = "${server.endpoint}"
                    }
                }
                """.trimIndent(),
            )
            .run("publishCurseforge")
        server.close()

        val metadata = server.api.lastMetadata!!

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforge")!!.outcome)
        assertEquals("Hello!", metadata.changelog)
        assertEquals("Test Upload", metadata.displayName)
        assertContains(metadata.gameVersions, 9990) // 1.20.1
        assertContains(metadata.gameVersions, 7499) // Fabric
        assertEquals("fabric-api", metadata.relations!!.projects[0].slug)
        assertEquals("mod-test", metadata.relations!!.projects[1].slug)
        assertEquals("mod-test-2", metadata.relations!!.projects[2].slug)
    }

    @Test
    fun uploadCurseforgeWithOptions() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest()
            .buildScript(
                """
                val fabricJar = tasks.register("fabricJar", Jar::class.java) {
                    archiveClassifier = "fabric"
                }
                val forgeJar = tasks.register("forgeJar", Jar::class.java) {
                    archiveClassifier = "forge"
                }

                publishMods {
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                
                    // Common options that can be re-used between diffrent curseforge tasks
                    val curseforgeOptions = curseforgeOptions {
                        accessToken = "123"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    curseforge("curseforgeFabric") {
                        from(curseforgeOptions)
                        file = fabricJar.flatMap { it.archiveFile }
                        projectId = "123456"
                        modLoaders.add("fabric")
                        requires {
                            slug = "fabric-api"
                        }
                    }
                    
                    curseforge("curseforgeForge") {
                        from(curseforgeOptions)
                        file = forgeJar.flatMap { it.archiveFile }
                        projectId = "789123"
                        modLoaders.add("forge")
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforgeFabric")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforgeForge")!!.outcome)
        assertContains(server.api.files, "mpp-example-forge.jar")
        assertContains(server.api.files, "mpp-example-fabric.jar")
    }

    // Also test in groovy to ensure that the closures are working as expected
    @Test
    fun uploadCurseforgeWithOptionsGroovy() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest(groovy = true)
            .buildScript(
                """
                tasks.register("fabricJar", Jar) {
                    archiveClassifier = "fabric"
                }
                tasks.register("forgeJar", Jar) {
                    archiveClassifier = "forge"
                }

                publishMods {
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                
                    // Common options that can be re-used between diffrent curseforge tasks
                    def curseforgeOptions = curseforgeOptions {
                        accessToken = "123"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    curseforge("curseforgeFabric") {
                        from curseforgeOptions
                        file = fabricJar.archiveFile
                        projectId = "123456"
                        modLoaders.add("fabric")
                        requires {
                            slug = "fabric-api"
                        }
                    }
                    
                    curseforge("curseforgeForge") {
                        from curseforgeOptions
                        file = forgeJar.archiveFile
                        projectId = "789123"
                        modLoaders.add("forge")
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforgeFabric")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforgeForge")!!.outcome)
        assertContains(server.api.files, "mpp-example-forge.jar")
        assertContains(server.api.files, "mpp-example-fabric.jar")
    }

    @Test
    fun dryRunCurseforge() {
        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                    modLoaders.add("fabric")
                    
                    dryRun = true
                
                    curseforge {
                        accessToken = providers.environmentVariable("TEST_TOKEN_THAT_DOES_NOT_EXISTS")
                        projectId = "123456"
                        minecraftVersions.add("1.20.1")
                    }
                }
                """.trimIndent(),
            )
            .run("publishCurseforge")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforge")!!.outcome)
    }

    @Test
    fun uploadCurseforgeError() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                    modLoaders.add("fabric")
                
                    curseforge {
                        accessToken = "abc"
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

        assertEquals(TaskOutcome.FAILED, result.task(":publishCurseforge")!!.outcome)
        result.output.contains("Request failed, status: 401 message: You must provide an API token using the `X-Api-Token` header")
    }

    @Test
    fun uploadCurseforgeNoDeps() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest()
            .buildScript(
                """
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
                        
                        apiEndpoint = "${server.endpoint}"
                    }
                }
                """.trimIndent(),
            )
            .run("publishCurseforge")
        server.close()

        val metadata = server.api.lastMetadata!!

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforge")!!.outcome)
        assertEquals(null, metadata.relations)
        assertEquals("mpp-example 1.0.0", metadata.displayName)
    }

    // Test to ensure that a version is set
    @Test
    fun uploadCurseforgeNoVersionError() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    type = BETA
                    modLoaders.add("fabric")
                
                    curseforge {
                        accessToken = "abc"
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

        assertEquals(TaskOutcome.FAILED, result.task(":publishCurseforge")!!.outcome)
        result.output.contains("Gradle version is unspecified")
    }
}
