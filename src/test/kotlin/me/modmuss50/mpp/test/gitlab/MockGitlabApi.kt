package me.modmuss50.mpp.test.gitlab

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import me.modmuss50.mpp.test.MockWebServer

class MockGitlabApi : MockWebServer.MockApi {
    override fun routes(): EndpointGroup =
        EndpointGroup {
            path("projects") {
                path("{projectId}") {
                    get(this::getProject)
                    path("releases") {
                        post(this::createRelease)
                        get("{tagName}", this::getRelease)
                        put("{tagName}", this::updateRelease)
                    }
                    path("uploads") {
                        post(this::uploadAsset)
                    }
                }
            }
    }

    private fun getProject(
        context: Context
    ) {
        val id = context.pathParam("projectId")
        context.result(
            """
            {
              "id": $id,
              "name": "mock-project",
              "path_with_namespace": "mock/namespace/mock-project"
            }
            """.trimIndent()
        )
    }

    private fun createRelease(
        context: Context
    ) {
        val tagName = context.queryParam("tag_name") ?: "v1.0"
        val projectId = context.pathParam("projectId")
        context.result(
            """
            {
              "tag_name": "$tagName",
              "name": "Release $tagName",
              "description": "Mock release for project $projectId",
              "assets": { "links": [] }
            }
            """.trimIndent()
        )
    }

    private fun updateRelease(
        context: Context
    ) {
        val tagName = context.pathParam("tagName")
        val projectId = context.pathParam("projectId")
        context.result(
            """
            {
              "tag_name": "$tagName",
              "name": "Updated Release $tagName",
              "description": "Updated mock release for project $projectId",
              "assets": { "links": [] }
            }
            """.trimIndent()
        )
    }

    private fun getRelease(
        context: Context
    ) {
        val tagName = context.pathParam("tagName")
        val projectId = context.pathParam("projectId")
        context.result(
            """
            {
              "tag_name": "$tagName",
              "name": "Release $tagName",
              "description": "Mock release for project $projectId",
              "assets": { "links": [] }
            }
            """.trimIndent()
        )
    }

    private fun uploadAsset(
        context: Context
    ) {
        val projectId = context.pathParam("projectId")
        val fileName = context.queryParam("name") ?: "mock-file.txt"
        context.result(
            """
        {
          "id": "1",
          "alt": "$fileName",
          "url": "http://localhost:${context.port()}/projects/$projectId/uploads/$fileName"
        }
        """.trimIndent()
        )
    }
}