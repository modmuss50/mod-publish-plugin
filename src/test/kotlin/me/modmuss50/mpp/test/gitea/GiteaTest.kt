package me.modmuss50.mpp.test.gitea

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class GiteaTest : IntegrationTest {
    @Test
    fun uploadGitea() {
        val server = MockWebServer(MockGiteaApi())

        val result = gradleTest()
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
            )
            .run("publishGitea")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitea")!!.outcome)
    }

    @Test
    fun uploadForgejo() {
        val server = MockWebServer(MockGiteaApi())

        val result = gradleTest()
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
            )
            .run("publishForgejo")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishForgejo")!!.outcome)
    }

    @Test
    fun noMainFile() {
        val server = MockWebServer(MockGiteaApi())

        val result = gradleTest()
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
            )
            .run("publishGitea")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitea")!!.outcome)
    }

    @Test
    fun uploadGiteaExistingRelease() {
        val server = MockWebServer(MockGiteaApi())

        val result = gradleTest()
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
            )
            .run("publishGiteaOther")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGiteaOther")!!.outcome)
    }

    @Test
    fun allowEmptyFiles() {
        val server = MockWebServer(MockGiteaApi())

        val result = gradleTest()
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
            )
            .subProject(
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
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitea")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":child:publishGitea")!!.outcome)
    }

    @Test
    fun allowEmptyFilesDryRun() {
        val server = MockWebServer(MockGiteaApi())

        val result = gradleTest()
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
            )
            .run("publishGitea")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitea")!!.outcome)
    }

    @Test
    fun failOnDuplicateNames() {
        val server = MockWebServer(MockGiteaApi())

        val result = gradleTest()
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
            )
            .run("publishGitea")
        server.close()

        assertEquals(TaskOutcome.FAILED, result.task(":publishGitea")!!.outcome)
        assertContains(result.output, "Gitea file names must be unique within a release, found duplicates: mpp-example.jar")
    }

    @Test
    fun failOnParentingGiteaWithForgejo() {
        val server = MockWebServer(MockGiteaApi())

        val result = gradleTest()
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
            )
            .run("publishGitea")
        server.close()

        assertEquals(TaskOutcome.FAILED, result.task(":publishGitea")!!.outcome)
        assertContains(result.output, "Unable to parent a Forgejo instance to a Gitea instance")
    }
}
