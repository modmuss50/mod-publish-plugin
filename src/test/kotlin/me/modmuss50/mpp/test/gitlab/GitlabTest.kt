package me.modmuss50.mpp.test.gitlab

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class GitlabTest : IntegrationTest {
    @Test
    fun uploadGitlab() {
        val server = MockWebServer(MockGitlabApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitlab {
                            accessToken = "123"
                            projectId = 1
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release-1.0.0"
                        }
                    }
                """.trimIndent()
            )
            .run("publishGitlab")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitlab")!!.outcome)
    }

    @Test
    fun noMainFile() {
        val server = MockWebServer(MockGitlabApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitlab {
                            accessToken = "123"
                            projectId = 1
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release-1.0.0"
                            additionalFiles.from(tasks.jar.flatMap { it.archiveFile })
                        }
                    }
                """.trimIndent()
            )
            .run("publishGitlab")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitlab")!!.outcome)
    }

    @Test
    fun uploadGitlabExistingRelease() {
        val server = MockWebServer(MockGitlabApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitlab {
                            accessToken = "123"
                            projectId = 1
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release-1.0.0"
                        }
                        gitlab("gitlabOther") {
                            accessToken = "123"
                            apiEndpoint = "${server.endpoint}"
                            parent(tasks.named("publishGitlab"))
                        }
                    }
                """.trimIndent()
            )
            .run("publishGitlabOther")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitlabOther")!!.outcome)
    }

    @Test
    fun allowEmptyFiles() {
        val server = MockWebServer(MockGitlabApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        gitlab {
                            accessToken = "123"
                            projectId = 1
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release-1.0.0"
                            allowEmptyFiles = true
                        }
                    }
                """.trimIndent()
            )
            .subProject(
                "child",
                """
                    publishMods {
                        gitlab {
                            accessToken = "123"
                            apiEndpoint = "${server.endpoint}"
                            parent(project(":").tasks.named("publishGitlab"))
                            file = tasks.jar.flatMap { it.archiveFile }
                        }
                    }
                """.trimIndent()
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitlab")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":child:publishGitlab")!!.outcome)
    }

    @Test
    fun allowEmptyFilesDryRun() {
        val server = MockWebServer(MockGitlabApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        dryRun = true
                        gitlab {
                            accessToken = "123"
                            projectId = 1
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release-1.0.0"
                            allowEmptyFiles = true
                        }
                    }
                """.trimIndent()
            )
            .run("publishGitlab")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGitlab")!!.outcome)
    }

    @Test
    fun failOnDuplicateNames() {
        val server = MockWebServer(MockGitlabApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = STABLE

                    gitlab {
                        accessToken = "123"
                        projectId = 1
                        commitish = "main"
                        apiEndpoint = "${server.endpoint}"
                        tagName = "release-1.0.0"

                        additionalFiles.from(tasks.jar.flatMap { it.archiveFile })
                    }
                }
            """.trimIndent()
            )
            .run("publishGitlab")
        server.close()

        assertEquals(TaskOutcome.FAILED, result.task(":publishGitlab")!!.outcome)
        assertContains(result.output, "GitLab file names must be unique within a release, found duplicates: mpp-example.jar")
    }
}