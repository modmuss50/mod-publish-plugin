package me.modmuss50.mpp.test.curseforge

import io.javalin.apibuilder.ApiBuilder.before
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.platforms.curseforge.CurseforgeApi
import me.modmuss50.mpp.test.MockWebServer
import java.io.BufferedReader

class MockCurseforgeApi : MockWebServer.MockApi {
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    var lastMetadata: CurseforgeApi.UploadFileMetadata? = null
    val files: ArrayList<String> = ArrayList()

    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("api") {
                path("game/version-types") {
                    before(this::authHandler)
                    get(this::versionTypes)
                }
                path("game/versions") {
                    before(this::authHandler)
                    get(this::versions)
                }
                path("projects/{projectId}/upload-file") {
                    before(this::authHandler)
                    post(this::uploadFile)
                }
            }
        }
    }

    private fun authHandler(context: Context) {
        val apiToken = context.header("X-Api-Token")

        if (apiToken != "123") {
            throw UnauthorizedResponse(
                """
                {
                  "errorCode": 401,
                  "errorMessage": "You must provide an API token using the `X-Api-Token` header, the `token` query string parameter, your email address and an API token using HTTP basic authentication."
                }
                """.trimIndent(),
            )
        }
    }

    private fun versionTypes(context: Context) {
        val versions = readResource("curseforge_version_types.json")
        context.result(versions)
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
        files.add(file.filename())

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
