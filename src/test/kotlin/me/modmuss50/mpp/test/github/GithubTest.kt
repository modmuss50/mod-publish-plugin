package me.modmuss50.mpp.test.github

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class GithubTest : IntegrationTest {
    @Test
    fun uploadGithub() {
        val server = MockWebServer(MockGithubApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        github {
                            accessToken = "123"
                            repository = "test/example"
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release/1.0.0"
                        }
                    }
                """.trimIndent(),
            )
            .run("publishGithub")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGithub")!!.outcome)
    }

    @Test
    fun noMainFile() {
        val server = MockWebServer(MockGithubApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        github {
                            accessToken = "123"
                            repository = "test/example"
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release/1.0.0"
                            additionalFiles.from(tasks.jar.flatMap { it.archiveFile })
                        }
                    }
                """.trimIndent(),
            )
            .run("publishGithub")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGithub")!!.outcome)
    }

    @Test
    fun uploadGithubExistingRelease() {
        val server = MockWebServer(MockGithubApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        file = tasks.jar.flatMap { it.archiveFile }
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        github {
                            accessToken = "123"
                            repository = "test/example"
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release/1.0.0"
                        }
                        github("githubOther") {
                            accessToken = "123"
                            apiEndpoint = "${server.endpoint}"
                            parent(tasks.named("publishGithub"))
                        }
                    }
                """.trimIndent(),
            )
            .run("publishGithubOther")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGithubOther")!!.outcome)
    }

    @Test
    fun allowEmptyFiles() {
        val server = MockWebServer(MockGithubApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        github {
                            accessToken = "123"
                            repository = "test/example"
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
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
                        github {
                            accessToken = "123"
                            apiEndpoint = "${server.endpoint}"
                            parent(project(":").tasks.named("publishGithub"))
                            file = tasks.jar.flatMap { it.archiveFile }
                        }
                    }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGithub")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":child:publishGithub")!!.outcome)
    }

    @Test
    fun allowEmptyFilesDryRun() {
        val server = MockWebServer(MockGithubApi())

        val result = gradleTest()
            .buildScript(
                """
                    publishMods {
                        changelog = "Hello!"
                        version = "1.0.0"
                        type = STABLE
                        dryRun = true
                        github {
                            accessToken = "123"
                            repository = "test/example"
                            commitish = "main"
                            apiEndpoint = "${server.endpoint}"
                            tagName = "release/1.0.0"
                            allowEmptyFiles = true
                        }
                    }
                """.trimIndent(),
            )
            .run("publishGithub")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGithub")!!.outcome)
    }
}
