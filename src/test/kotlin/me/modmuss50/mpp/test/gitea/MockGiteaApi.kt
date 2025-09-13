package me.modmuss50.mpp.test.gitea;

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import me.modmuss50.mpp.test.MockWebServer

class MockGiteaApi : MockWebServer.MockApi {
    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("api/v1/repos") {
                path("{owner}/{name}") {
                    path("releases") {
                        post(this::createRelease)
                        path("{id}/assets") {
                            post(this::uploadAsset)
                        }
                        get("{id}", this::getRelease)
                        patch("{id}", this::updateRelease)
                    }
                }
            }
        }
    }

    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoCreateRelease
    private fun createRelease(context: Context) {
        context.result(
            """
            {
            "id": 1,
            "upload_url": "http://localhost:${context.port()}/repos/${context.pathParam("owner")}/${context.pathParam("name")}/releases/1/assets",
            "html_url": "https://codeberg.org"
            }
            """.trimIndent(),
        )
    }

    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoCreateReleaseAttachment
    private fun uploadAsset(context: Context) {
        context.result(
            """
            {
            }
            """.trimIndent(),
        )
    }

    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoGetRelease
    private fun getRelease(context: Context) {
        val id = context.pathParam("id")
        context.result(
            """
            {
            "id": $id,
            "upload_url": "http://localhost:${context.port()}/repos/${context.pathParam("owner")}/${context.pathParam("name")}/releases/${id}/assets",
            "html_url": "https://codeberg.org"
            }
            """.trimIndent(),
        )
    }

    // https://docs.gitea.com/api/1.24/#tag/repository/operation/repoEditRelease
    private fun updateRelease(context: Context) {
        context.result(
            """
            {
            }
            """.trimIndent(),
        )
    }
}

