package me.modmuss50.mpp.test.misc

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import me.modmuss50.mpp.test.MockWebServer
import java.io.BufferedReader

class MockMinecraftApi : MockWebServer.MockApi {
    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("mc/game/version_manifest_v2.json") {
                get(this::versions)
            }
        }
    }

    private fun versions(context: Context) {
        val versions = readResource("version_manifest_v2.json")
        context.result(versions)
    }

    private fun readResource(path: String): String {
        this::class.java.classLoader!!.getResourceAsStream(path).use { inputStream ->
            BufferedReader(inputStream!!.reader()).use { reader ->
                return reader.readText()
            }
        }
    }
}
