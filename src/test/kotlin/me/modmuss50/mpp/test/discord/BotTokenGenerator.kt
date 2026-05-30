package me.modmuss50.mpp.test.discord

import kotlinx.serialization.Serializable
import me.modmuss50.mpp.networking.HttpApi.post
import me.modmuss50.mpp.networking.RequestContext
import java.io.File
import java.net.http.HttpRequest


val httpContext = RequestContext(
    json = RequestContext.Default.json,
    userAgent = RequestContext.Default.userAgent,
    client = RequestContext.Default.client,
    exceptionFactory = RequestContext.Default.exceptionFactory,
)

/*
Use this to generate a bot created webhook URL for testing the discord support
Get a bot token from https://discord.com/developers/applications and set it in options.json
Run this with the channel id as the first argument
 */
fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: BotTokenGenerator <channelId>")
        return
    }

    val options = httpContext.json.decodeFromString<Options>(File("options.json").readText())

    val channelId = args[0]
    val headers: Map<String, String> =
        mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bot ${options.discordBotToken}",
        )

    val request = CreateWebhookRequest("test")
    val body = HttpRequest.BodyPublishers.ofString(httpContext.json.encodeToString(request))
    val response = httpContext.post<CreateWebhookResponse>("https://discord.com/api/v9/channels/$channelId/webhooks", body, headers)

    println(response)
}

@Serializable
data class CreateWebhookRequest(
    val name: String,
)

@Serializable
data class CreateWebhookResponse(
    val url: String,
)

@Serializable
data class Options(
    val discordBotToken: String,
)
