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
                        }
                    }
                """.trimIndent(),
            )
            .run("publishGithub")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishGithub")!!.outcome)
    }
}
