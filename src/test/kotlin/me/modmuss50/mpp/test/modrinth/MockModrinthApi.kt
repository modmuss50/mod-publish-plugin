package me.modmuss50.mpp.test.modrinth

import io.javalin.apibuilder.ApiBuilder.before
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.platforms.modrinth.ModrinthApi
import me.modmuss50.mpp.test.MockWebServer

class MockModrinthApi : MockWebServer.MockApi {
    val json = Json { ignoreUnknownKeys = true }
    var lastCreateVersion: ModrinthApi.CreateVersion? = null
    var projectBody: String? = null

    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("/project/{slug}/version") {
                get(this::listVersions)
            }
            path("/version") {
                before(this::authHandler)
                post(this::createVersion)
            }
            path("project/{slug}") {
                patch(this::modifyProject)
            }
            path("/project/{slug}/check") {
                get(this::checkProject)
            }
        }
    }
    
    private fun listVersions(context: Context) {
        context.result(
            json.encodeToString(
                arrayOf(
                    ModrinthApi.ListVersionsResponse(
                        "0.92.2+1.20.1",
                        "P7uGFii0"
                    ),
                    ModrinthApi.ListVersionsResponse(
                        "0.92.1+1.20.1",
                        "ba99D9Qf"
                    )
                )
            )
        )
    }

    private fun authHandler(context: Context) {
        val apiToken = context.header("Authorization")

        if (apiToken != "123") {
            throw UnauthorizedResponse("Invalid access token")
        }
    }

    private fun createVersion(context: Context) {
        val data = context.formParam("data")
            ?: throw BadRequestResponse("No metadata")

        val createVersion = json.decodeFromString<ModrinthApi.CreateVersion>(data)
        lastCreateVersion = createVersion

        for (filePart in createVersion.fileParts) {
            context.uploadedFile(filePart)
                ?: throw BadRequestResponse("No file")
        }

        context.result(
            json.encodeToString(
                ModrinthApi.CreateVersionResponse(
                    id = "hFdJG9fY",
                    projectId = createVersion.projectId,
                    authorId = "JZA4dW8o",
                ),
            ),
        )
    }

    private fun modifyProject(context: Context) {
        val modifyProject = json.decodeFromString<ModrinthApi.ModifyProject>(context.body())
        projectBody = modifyProject.body
        context.result()
    }

    private fun checkProject(context: Context) {
        context.result(
            """
        {
            "id": "AABBCCDD"
        }
            """.trimIndent(),
        )
    }
}
