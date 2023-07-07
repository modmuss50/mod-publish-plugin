package me.modmuss50.mpp.test

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import java.io.BufferedReader

class MockCurseForgeApi : MockWebServer.MockApi {
    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("api") {
                path("game/versions") {
                    get(this::versions)
                }
                path("projects/{projectId}/upload-file") {
                    post(this::uploadFile)
                }
            }
        }
    }

    private fun versions(context: Context) {
        val versions = readResource("curseforge_versions.json")
        context.result(versions)
    }

    private fun uploadFile(context: Context) {
    }

    private fun readResource(path: String): String {
        this::class.java.classLoader!!.getResourceAsStream(path).use { inputStream ->
            BufferedReader(inputStream!!.reader()).use { reader ->
                return reader.readText()
            }
        }
    }
}
