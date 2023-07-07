package me.modmuss50.mpp.test

import io.javalin.Javalin
import io.javalin.apibuilder.EndpointGroup

class MockWebServer(val api: MockApi) : AutoCloseable {
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
}
