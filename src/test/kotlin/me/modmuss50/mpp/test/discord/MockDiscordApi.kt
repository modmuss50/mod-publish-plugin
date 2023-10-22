package me.modmuss50.mpp.test.discord

import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.platforms.discord.DiscordAPI
import me.modmuss50.mpp.test.MockWebServer

class MockDiscordApi : MockWebServer.MockApi {
    var request: DiscordAPI.Webhook? = null

    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("api/webhooks/{key}/{token}") {
                post(this::webhook)
            }
        }
    }

    private fun webhook(context: Context) {
        request = Json.decodeFromString(context.body())
        context.result("") // Just returns an empty string
    }
}
