package me.modmuss50.mpp.test.curseforge

import io.javalin.apibuilder.ApiBuilder.before
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.platforms.curseforge.CurseforgeApi
import me.modmuss50.mpp.test.MockWebServer
import java.io.BufferedReader

class MockCurseforgeApi : MockWebServer.MockApi {
    val json = Json { ignoreUnknownKeys = true }
    var lastMetadata: CurseforgeApi.UploadFileMetadata? = null

    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("api") {
                before(this::authHandler)
                path("game/versions") {
                    get(this::versions)
                }
                path("projects/{projectId}/upload-file") {
                    post(this::uploadFile)
                }
            }
        }
    }

    private fun authHandler(context: Context) {
        val apiToken = context.header("X-Api-Token")

        if (apiToken != "123") {
            throw UnauthorizedResponse("Invalid access token")
        }
    }

    private fun versions(context: Context) {
        val versions = readResource("curseforge_versions.json")
        context.result(versions)
    }

    private fun uploadFile(context: Context) {
        val metadata = context.formParam("metadata")
        val file = context.uploadedFile("file")

        if (metadata == null) {
            throw BadRequestResponse("No metadata")
        }

        if (file == null) {
            throw BadRequestResponse("No file")
        }

        lastMetadata = json.decodeFromString(metadata)

        context.result("""{"id": "20402"}""")
    }

    private fun readResource(path: String): String {
        this::class.java.classLoader!!.getResourceAsStream(path).use { inputStream ->
            BufferedReader(inputStream!!.reader()).use { reader ->
                return reader.readText()
            }
        }
    }
}
