package me.modmuss50.mpp.test.gitea

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GiteaTest : IntegrationTest {
    private lateinit var server: MockWebServer<MockGiteaApi>

    @BeforeTest
    fun setup() {
        server = MockWebServer(MockGiteaApi())
    }

    @AfterTest
    fun cleanup() {
        server.close()
    }

    @Test
    fun uploadGitea() {
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitea {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                        }
                    }
                    """.trimIndent(),
                ).run("publishGitea")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitea")!!.outcome)
    }

    @Test
    fun uploadForgejo() {
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        forgejo {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                        }
                    }
                    """.trimIndent(),
                ).run("publishForgejo")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishForgejo")!!.outcome)
    }

    @Test
    fun uploadCodeberg() {
        // host(...) call is not needed in production repos since the apiEndpoint is static
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        codeberg {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                        }
                    }
                    """.trimIndent(),
                ).run("publishCodeberg")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCodeberg")!!.outcome)
    }

    @Test
    fun noMainFile() {
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitea {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                            additionalFiles.from(tasks.jar.flatMap { it.archiveFile })
                        }
                    }
                    """.trimIndent(),
                ).run("publishGitea")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitea")!!.outcome)
    }

    @Test
    fun uploadGiteaExistingRelease() {
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitea {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                        }
                        gitea("giteaOther") {
                            accessToken = "123"
                            parent(tasks.named("publishGitea"))
                        }
                    }
                    """.trimIndent(),
                ).run("publishGiteaOther")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGiteaOther")!!.outcome)
    }

    @Test
    fun allowEmptyFiles() {
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitea {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                            allowEmptyFiles = true
                        }
                    }
                    """.trimIndent(),
                ).subProject(
                    "child",
                    """
                    publishMods {
                        gitea {
                            accessToken = "123"
                            parent(project(":").tasks.named("publishGitea"))
                            file = tasks.jar.flatMap { it.archiveFile }
                        }
                    }
                    """.trimIndent(),
                ).run("publishMods")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitea")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":child:publishGitea")!!.outcome)
    }

    @Test
    fun allowEmptyFilesDryRun() {
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        dryRun = true
                        gitea {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                            allowEmptyFiles = true
                        }
                    }
                    """.trimIndent(),
                ).run("publishGitea")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitea")!!.outcome)
    }

    @Test
    fun failOnDuplicateNames() {
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitea {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                            additionalFiles.from(tasks.jar.flatMap { it.archiveFile })
                        }
                    }
                    """.trimIndent(),
                ).run("publishGitea")

        assertEquals(TaskOutcome.FAILED, result.task(":publishGitea")!!.outcome)
        assertContains(result.output, "Gitea file names must be unique within a release, found duplicates: mpp-example.jar")
    }

    @Test
    fun failOnParentingGiteaWithForgejo() {
        val result =
            gradleTest()
                .buildScript(
                    """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitea {
                            accessToken = "123"
                            host(uri("${server.endpoint}"))
                            repository = "test/example"
                            commitish = "main"
                            tagName = "release/1.0.0"
                            additionalFiles.from(tasks.jar.flatMap { it.archiveFile })
                        }
                        forgejo {
                            accessToken = "123"
                            parent(tasks.named("publishGitea"))
                        }
                    }
                    """.trimIndent(),
                ).run("publishGitea")

        assertNull(result.task(":publishGitea"))
        assertContains(result.output, "Unable to make a Gitea instance a parent of a Forgejo instance")
    }
}
