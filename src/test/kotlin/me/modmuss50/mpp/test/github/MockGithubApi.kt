package me.modmuss50.mpp.test.github

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import me.modmuss50.mpp.test.MockWebServer

// The very bare minimum to mock out the GitHub API.
class MockGithubApi(
    private val existingTag: Boolean = false,
) : MockWebServer.MockApi {
    val uploadedAssetNames = mutableListOf<String>()
    var createdReleases = 0
    var createReleaseBody: String? = null

    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("repos") {
                path("{owner}/{name}") {
                    get(this::getRepo)
                    get("git/ref/tags/{tag}", this::getTag)
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

    // https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#get-a-repository
    private fun getRepo(context: Context) {
        context.result(
            """
            {
            "full_name": "${context.pathParam("owner")}/${context.pathParam("name")}"
            }
            """.trimIndent(),
        )
    }

    // https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28#create-a-release
    private fun createRelease(context: Context) {
        createdReleases++
        createReleaseBody = context.body()
        context.result(
            """
            {
            "id": 1,
            "upload_url": "http://localhost:${context.port()}/repos/${context.pathParam("owner")}/${context.pathParam("name")}/releases/1/assets{?name,label}",
            "html_url": "https://github.com"
            }
            """.trimIndent(),
        )
    }

    private fun getTag(context: Context) {
        if (existingTag) {
            context.result("{}")
        } else {
            context.status(404)
        }
    }

    // https://docs.github.com/en/rest/releases/assets?apiVersion=2022-11-28#upload-a-release-asset
    private fun uploadAsset(context: Context) {
        uploadedAssetNames.add(context.queryParam("name")!!)
        context.result(
            """
            {
            }
            """.trimIndent(),
        )
    }

    // https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28#get-a-release
    private fun getRelease(context: Context) {
        val id = context.pathParam("id")
        context.result(
            """
            {
            "id": $id,
            "upload_url": "http://localhost:${context.port()}/repos/${context.pathParam("owner")}/${context.pathParam("name")}/releases/1/assets{?name,label}",
            "html_url": "https://github.com"
            }
            """.trimIndent(),
        )
    }

    private fun updateRelease(context: Context) {
        val id = context.pathParam("id")
        context.result(
            """
            {
            "id": $id,
            "upload_url": "http://localhost:${context.port()}/repos/${context.pathParam("owner")}/${context.pathParam("name")}/releases/$id/assets{?name,label}",
            "html_url": "https://github.com"
            }
            """.trimIndent(),
        )
    }
}
