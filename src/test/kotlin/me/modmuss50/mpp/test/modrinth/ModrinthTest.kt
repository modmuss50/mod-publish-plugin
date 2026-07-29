package me.modmuss50.mpp.test.modrinth

import me.modmuss50.mpp.platforms.modrinth.ModrinthApi
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

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
        val api = MockModrinthApi()
        val server = MockWebServer(api)

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

        assertNull(api.lastCreateVersion!!.environment, "Environment should be null when not specified")
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
    fun uploadModrinthMinecraftVersionList() {
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

                    minecraftVersionList("26.1, 26.1.1, 26.1.2, 26.2-snapshot-1")

                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        val gameVersions = mockModrinthApi.lastCreateVersion!!.gameVersions
        assertContains(gameVersions, "26.1")
        assertContains(gameVersions, "26.1.1")
        assertContains(gameVersions, "26.1.2")
        assertContains(gameVersions, "26.2-snapshot-1")
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
        assertFalse(gameVersions.contains("23w44a"))
    }

    @Test
    fun uploadModrinthMinecraftVersionRangeLatestRelease() {
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
                        end = "latestRelease"
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
    fun invalidDependency() {
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
                        // neither id nor slug set
                    }
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")

        assertContains(result.output, "Modrinth dependency must have either an id or slug specified")
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

    @Test
    fun uploadModrinthEnvironment() {
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
                    environment = CLIENT_ONLY
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        assertEquals(ModrinthEnvironment.CLIENT_ONLY, api.lastCreateVersion!!.environment)
    }

    @Test
    fun uploadModrinthSourcesJarType() {
        val api = MockModrinthApi()
        val server = MockWebServer(api)

        val result = gradleTest()
            .buildScript(
                """
            val sourcesJar = tasks.register("sourcesJar", Jar::class.java) {
                archiveClassifier.set("sources")
            }
            
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
                    
                    additionalFile(sourcesJar) {
                        type = SOURCES_JAR
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        assertEquals(ModrinthApi.AdditionalFileType.SOURCES_JAR, api.lastCreateVersion!!.fileTypes!!["file_0"])
    }

    @Test
    fun uploadModrinthJavadocJarType() {
        val api = MockModrinthApi()
        val server = MockWebServer(api)

        val result = gradleTest()
            .file("mod-1.0.0-javadoc.jar", "dummy")
            .buildScript(
                """
            val javadocJar = tasks.register("javadocJar", Jar::class.java) {
                archiveClassifier.set("javadoc")
            }
            
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
                    
                    additionalFile(javadocJar.flatMap { it.archiveFile }) {
                        type = JAVADOC_JAR
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        assertEquals(ModrinthApi.AdditionalFileType.JAVADOC_JAR, api.lastCreateVersion!!.fileTypes!!["file_0"])
    }

    @Test
    fun uploadModrinthSignatureType() {
        val api = MockModrinthApi()
        val server = MockWebServer(api)

        val result = gradleTest()
            .buildScript(
                """
            val signature = tasks.register("signature", Jar::class.java) {
                archiveClassifier.set("sig")
            }
            
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
                    
                    additionalFile(signature.flatMap { it.archiveFile }) {
                        type = SIGNATURE
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        assertEquals(ModrinthApi.AdditionalFileType.SIGNATURE, api.lastCreateVersion!!.fileTypes!!["file_0"])
    }

    @Test
    fun uploadModrinthAdditionalFileWithoutType() {
        val api = MockModrinthApi()
        val server = MockWebServer(api)

        val result = gradleTest()
            .buildScript(
                """
            val unknownFile = tasks.register("unknownFile", Jar::class.java) {
                archiveClassifier.set("unknown")
            }
            
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
                    
                    additionalFiles.from(unknownFile)
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        assert(api.lastCreateVersion!!.fileTypes.isNullOrEmpty())
    }

    @Test
    fun uploadModrinthMultipleAdditionalFileTypes() {
        val api = MockModrinthApi()
        val server = MockWebServer(api)

        val result = gradleTest()
            .buildScript(
                """
            val sourcesJar = tasks.register("sourcesJar", Jar::class.java) {
                archiveClassifier.set("sources")
            }
            val javadocJar = tasks.register("javadocJar", Jar::class.java) {
                archiveClassifier.set("javadoc")
            }
            val signature = tasks.register("signature", Jar::class.java) {
                archiveClassifier.set("sig")
            }
            
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
                    
                    additionalFile(sourcesJar.flatMap { it.archiveFile }) {
                        type = SOURCES_JAR
                    }
                    additionalFile(javadocJar.flatMap { it.archiveFile }) {
                        type = JAVADOC_JAR
                    }
                    additionalFile(signature.flatMap { it.archiveFile }) {
                        type = SIGNATURE
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .notConfigCacheCompatible()
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        val fileTypes = api.lastCreateVersion!!.fileTypes!!
        assertEquals(
            setOf(
                ModrinthApi.AdditionalFileType.SOURCES_JAR,
                ModrinthApi.AdditionalFileType.JAVADOC_JAR,
                ModrinthApi.AdditionalFileType.SIGNATURE,
            ),
            fileTypes.values.toSet(),
        )
        assertFalse(fileTypes.containsKey("primaryFile"))
    }

    @Test
    fun uploadModrinthAdditionalFilesDeduplication() {
        val api = MockModrinthApi()
        val server = MockWebServer(api)

        val result = gradleTest()
            .buildScript(
                """
            val sourcesJar = tasks.register("sourcesJar", Jar::class.java) {
                archiveClassifier.set("sources")
            }
            val javadocJar = tasks.register("javadocJar", Jar::class.java) {
                archiveClassifier.set("javadoc")
            }
            
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
                
                additionalFiles.from(sourcesJar, javadocJar)
                
                modrinth {
                    accessToken = "123"
                    projectId = "12345678"
                    minecraftVersions.add("1.20.1")
                    
                    additionalFile(sourcesJar.flatMap { it.archiveFile }) {
                        type = SOURCES_JAR
                    }
                    additionalFile(javadocJar.flatMap { it.archiveFile }) {
                        type = JAVADOC_JAR
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .notConfigCacheCompatible()
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        assertEquals(3, api.lastCreateVersion!!.fileParts.size)
        val fileTypes = api.lastCreateVersion!!.fileTypes!!
        assertEquals(2, fileTypes.size)
        assertEquals(
            setOf(
                ModrinthApi.AdditionalFileType.SOURCES_JAR,
                ModrinthApi.AdditionalFileType.JAVADOC_JAR,
            ),
            fileTypes.values.toSet(),
        )
        assertFalse(fileTypes.containsKey("primaryFile"))
    }

    @Test
    fun uploadModrinthNoAdditionalFilesEmptyFileTypes() {
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
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
        assert(api.lastCreateVersion!!.fileTypes.isNullOrEmpty())
    }
}
