package me.modmuss50.mpp.platforms.discord

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.HttpUtils
import okhttp3.RequestBody.Companion.toRequestBody

object DiscordAPI {
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json { explicitNulls = false }
    private val httpUtils = HttpUtils()
    private val headers: Map<String, String> = mapOf("Content-Type" to "application/json")

    // https://discord.com/developers/docs/resources/webhook#execute-webhook
    fun executeWebhook(url: String, webhook: Webhook) {
        val body = json.encodeToString(webhook).toRequestBody()
        httpUtils.post<String>(url, body, headers)
    }

    // https://discord.com/developers/docs/resources/webhook#execute-webhook-jsonform-params
    @Serializable
    data class Webhook(
        val content: String? = null,
        val username: String? = null,
        @SerialName("avatar_url")
        val avatarUrl: String? = null,
        val tts: Boolean? = null,
        val embeds: List<Embed>? = null,
        // allowedMentions -- Skip these as we dont need them
        // components
        // files
        // payload_json
        // attachments
        val flags: Int? = null,
        @SerialName("thread_name")
        val threadName: String? = null,
    )

    // https://discord.com/developers/docs/resources/channel#embed-object
    @Serializable
    data class Embed(
        val title: String? = null,
        val type: String? = null,
        val description: String? = null,
        val url: String? = null,
        val timestamp: String? = null, // ISO8601 timestamp
        val color: Int? = null,
        val footer: EmbedFooter? = null,
        val image: EmbedImage? = null,
        val thumbnail: EmbedThumbnail? = null,
        val video: EmbedVideo? = null,
        val provider: EmbedProvider? = null,
        val author: EmbedAuthor? = null,
        val fields: List<EmbedField>? = null,
    )

    // https://discord.com/developers/docs/resources/channel#embed-object-embed-footer-structure
    @Serializable
    data class EmbedFooter(
        val text: String,
        @SerialName("icon_url")
        val iconUrl: String? = null,
        @SerialName("proxy_icon_url")
        val proxyIconUrl: String? = null,
    )

    // https://discord.com/developers/docs/resources/channel#embed-object-embed-image-structure
    @Serializable
    data class EmbedImage(
        val url: String,
        @SerialName("proxy_url")
        val proxyUrl: String? = null,
        val height: Int? = null,
        val width: Int? = null,
    )

    // https://discord.com/developers/docs/resources/channel#embed-object-embed-thumbnail-structure
    @Serializable
    data class EmbedThumbnail(
        val url: String,
        @SerialName("proxy_url")
        val proxyUrl: String? = null,
        val height: Int? = null,
        val width: Int? = null,
    )

    // https://discord.com/developers/docs/resources/channel#embed-object-embed-video-structure
    @Serializable
    data class EmbedVideo(
        val url: String? = null,
        @SerialName("proxy_url")
        val proxyUrl: String? = null,
        val height: Int? = null,
        val width: Int? = null,
    )

    // https://discord.com/developers/docs/resources/channel#embed-object-embed-provider-structure
    @Serializable
    data class EmbedProvider(
        val name: String? = null,
        val url: String? = null,
    )

    // https://discord.com/developers/docs/resources/channel#embed-object-embed-author-structure
    @Serializable
    data class EmbedAuthor(
        val name: String,
        val url: String? = null,
        @SerialName("icon_url")
        val iconUrl: String? = null,
        @SerialName("proxy_icon_url")
        val proxyIconUrl: String? = null,
    )

    // https://discord.com/developers/docs/resources/channel#embed-object-embed-field-structure
    @Serializable
    data class EmbedField(
        val name: String,
        val value: String,
        val inline: Boolean? = null,
    )
}
