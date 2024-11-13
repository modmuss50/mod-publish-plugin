package me.modmuss50.mpp.test.discord

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.HttpUtils
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

val json = Json { ignoreUnknownKeys = true }
val httpUtils = HttpUtils()

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

    val options = json.decodeFromString<Options>(File("options.json").readText())

    val channelId = args[0]
    val headers: Map<String, String> = mapOf(
        "Content-Type" to "application/json",
        "Authorization" to "Bot ${options.discordBotToken}",
    )

    val request = CreateWebhookRequest("test")
    val body = json.encodeToString(request).toRequestBody()
    val response = httpUtils.post<CreateWebhookResponse>("https://discord.com/api/v9/channels/$channelId/webhooks", body, headers)

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
