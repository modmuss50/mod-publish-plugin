package me.modmuss50.mpp.test

import io.javalin.Javalin
import io.javalin.apibuilder.EndpointGroup

class MockWebServer<T : MockWebServer.MockApi>(val api: T) : AutoCloseable {
    private val server: Javalin = Javalin.create()
        .routes(api.routes())
        .start(9082)

    val endpoint: String
        get() = "http://localhost:9082"

    override fun close() {
        server.stop()
    }

    interface MockApi {
        fun routes(): EndpointGroup
    }

    class CombinedApi(val apis: List<MockApi>) : MockApi {
        override fun routes(): EndpointGroup {
            return EndpointGroup {
                for (api in apis) {
                    api.routes().addEndpoints()
                }
            }
        }
    }
}
