package me.modmuss50.mpp.test.discord

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.platforms.discord.DiscordAPI
import me.modmuss50.mpp.test.MockWebServer

class MockDiscordApi : MockWebServer.MockApi {
    private val json = Json { classDiscriminator = "class"; encodeDefaults = true }
    var requests = arrayListOf<DiscordAPI.Webhook>()
    var requestedKeys = arrayListOf<String>()

    override fun routes(): EndpointGroup {
        return EndpointGroup {
            path("api/webhooks/{key}/{token}") {
                post(this::postWebhook)
                get(this::getWebhook)
            }
        }
    }

    private fun postWebhook(context: Context) {
        requests.add(json.decodeFromString(context.body()))
        requestedKeys.add(context.pathParam("key"))
        context.result("") // Just returns an empty string
    }

    private fun getWebhook(context: Context) {
        // Just returns a simple response so the component check passes
        context.result("{\"application_id\": \"0\"}")
    }
}
