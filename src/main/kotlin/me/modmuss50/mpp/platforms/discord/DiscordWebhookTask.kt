package me.modmuss50.mpp.platforms.discord

import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.modPublishExtension
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.annotations.ApiStatus
import javax.inject.Inject

interface DiscordWebhookOptions {
    @get:Input
    val webhookUrl: Property<String>

    @get:Input
    @get:Optional
    val dryRunWebhookUrl: Property<String>

    @get:Input
    val username: Property<String>

    @get:Input
    @get:Optional
    val avatarUrl: Property<String>

    @get:Input
    val content: Property<String>

    @get:Input
    val embedContent: Property<String>

    @get:Nested
    val style: Property<MessageStyle>

    fun from(other: DiscordWebhookOptions) {
        webhookUrl.convention(other.webhookUrl)
        dryRunWebhookUrl.convention(other.dryRunWebhookUrl)
        username.convention(other.username)
        avatarUrl.convention(other.avatarUrl)
        content.convention(other.content)
        embedContent.convention(other.embedContent)
        style.convention(other.style)
    }

    fun style(style: Action<MessageStyle>) {
        style.execute(this.style.get())
    }
}

@Suppress("MemberVisibilityCanBePrivate")
interface MessageStyle {
    @get:Input
    @get:Optional
    val thumbnailUrl: Property<String>

    @get:Input
    @get:Optional
    val color: Property<String>

    @get:Input
    @get:Optional
    val link: Property<String>

    fun from(other: MessageStyle) {
        thumbnailUrl.convention(other.thumbnailUrl)
        color.convention(other.color)
        link.convention(other.link)
    }
}

enum class LinkType {
    EMBED,
    BUTTON,
    INLINE,
}

@DisableCachingByDefault(because = "Publish webhook each time")
abstract class DiscordWebhookTask : DefaultTask(), DiscordWebhookOptions {
    @get:ApiStatus.Internal
    @get:Input
    abstract val dryRun: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val publishResults: ConfigurableFileCollection

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    init {
        group = "publishing"
        username.convention("Mod Publish Plugin")
        content.convention(project.modPublishExtension.changelog)
        embedContent.convention("")

        dryRun.set(project.modPublishExtension.dryRun)
        dryRun.finalizeValue()

        with(project.objects.newInstance(MessageStyle::class.java)) {
            link.convention("EMBED")

            style.convention(this)
        }

        // By default, announce all the platforms.
        publishResults.from(
            project.modPublishExtension.platforms
                .map { platform -> project.tasks.getByName(platform.taskName) as PublishModTask }
                .map { task -> task.result },
        )
    }

    /**
     * Set the platforms to announce.
     */
    fun setPlatforms(vararg platforms: Platform) {
        publishResults.setFrom(
            platforms
                .map { platform -> project.tasks.getByName(platform.taskName) as PublishModTask }
                .map { task -> task.result },
        )
    }

    /**
     * Set the platforms to announce, by passing in publish tasks.
     */
    fun setPlatforms(vararg tasks: TaskProvider<out Task>) {
        publishResults.setFrom(
            tasks
                .map { task -> task.map { it as PublishModTask } }
                .map { task -> task.flatMap { it.result } },
        )

        setPlatforms(project.tasks.containerWithType(PublishModTask::class.java))
    }

    /**
     * Set the platforms to announce, by passing in publish tasks.
     */
    fun setPlatforms(tasks: NamedDomainObjectCollection<out Task>) {
        publishResults.setFrom(
            tasks
                .map { it as PublishModTask }
                .map { it.result },
        )
    }

    /**
     * Set the platforms to announce, by passing in projects, using all the mod publish tasks from each project.
     */
    fun setPlatformsAllFrom(vararg projects: Project) {
        publishResults.setFrom(
            projects
                .map { it.tasks.withType(PublishModTask::class.java) }
                .flatMap { it.toList() }
                .map { it.result },
        )
    }

    @TaskAction
    fun announce() {
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(DiscordWorkAction::class.java) {
            it.from(this)
            it.publishResults.setFrom(publishResults)
            it.dryRun.set(dryRun)
            it.style.set(style)
        }
    }

    interface DiscordWorkParameters : WorkParameters, DiscordWebhookOptions {
        val publishResults: ConfigurableFileCollection

        val dryRun: Property<Boolean>
    }

    @Suppress("MemberVisibilityCanBePrivate")
    abstract class DiscordWorkAction : WorkAction<DiscordWorkParameters> {
        override fun execute() {
            with(parameters) {
                if (dryRun.get() && !dryRunWebhookUrl.isPresent) {
                    // Don't announce if we're dry running and don't have a dry run webhook URL.
                    return
                }

                val url = if (dryRun.get()) dryRunWebhookUrl else webhookUrl

                if (LinkType.valueOf(style.get().link.get()) == LinkType.BUTTON) {
                    // Verify that the webhook is application owned,
                    // as only ones made by an application can use components/buttons
                    val webhook = DiscordAPI.getWebhook(url.get())
                    if (webhook.applicationId == null) {
                        throw UnsupportedOperationException("Button links require the use of an application owned webhook")
                    }
                }

                // Get all the embeds used on the message
                val embeds = createEmbeds()

                // Get all components used on the message
                // A message can only have 5 action rows, so we split if needed
                val components = createComponents().chunked(5).iterator()

                var firstRequest = true

                // Only include plain text content if it is non-blank
                val messageContent = createMessageBody().takeIf { it.isNotBlank() }

                // Split the embeds across multiple messages if needed
                val embedChunks = embeds.chunked(10)
                embedChunks.forEachIndexed { index, chunk ->
                    DiscordAPI.executeWebhook(
                        url.get(),
                        DiscordAPI.Webhook(
                            username = username.get(),
                            // Include plain text content alongside the embed only on the first message, and only if non-blank
                            content = if (index == 0) messageContent else null,
                            avatarUrl = avatarUrl.orNull,
                            embeds = chunk,
                            // Only the last embed should have buttons
                            components = if (index == embedChunks.lastIndex && components.hasNext()) {
                                components.next()
                            } else {
                                null
                            },
                        ),
                    )

                    firstRequest = false
                }

                // Send the remaining buttons that didn't fit on the last message
                components.forEachRemaining {
                    DiscordAPI.executeWebhook(
                        url.get(),
                        DiscordAPI.Webhook(
                            // Include plain text content on the first message if it wasn't already sent, and only if non-blank
                            content = if (firstRequest) messageContent else null,
                            username = username.get(),
                            avatarUrl = avatarUrl.orNull,
                            components = components.next(),
                        ),
                    )

                    firstRequest = false
                }

                // Message has no embeds nor buttons, and was not sent yet — send plain text content if non-blank
                if (firstRequest && messageContent != null) {
                    DiscordAPI.executeWebhook(
                        url.get(),
                        DiscordAPI.Webhook(
                            content = messageContent,
                            username = username.get(),
                            avatarUrl = avatarUrl.orNull,
                        ),
                    )

                    firstRequest = false
                }
            }
        }

        /**
         * Create the embeds used for the message
         * The list has the content and the links.
         *
         * Depending on the embed content, only the links may be present
         */
        fun createEmbeds(): List<DiscordAPI.Embed> {
            with(parameters) {
                return if (embedContent.get().isBlank()) {
                    createLinkEmbeds()
                } else {
                    listOf(createModernEmbed()) + createLinkEmbeds()
                }
            }
        }

        /**
         * Create the message embed using embedContent as the description
         */
        fun createModernEmbed(): DiscordAPI.Embed {
            with(parameters) {
                val style: MessageStyle = style.get()
                return DiscordAPI.Embed(
                    thumbnail = style.thumbnailUrl.map { DiscordAPI.EmbedThumbnail(url = it) }.orNull,
                    description = embedContent.get(),
                    color = style.color.map { parseColor(it) }.orNull,
                )
            }
        }

        private fun parseColor(str: String): Int {
            return when (str) {
                "modrinth" -> 0x1BD96A
                "github" -> 0xF6F0FC
                "curseforge" -> 0xF16436
                else -> parseHexStringOrThrow(str)
            }
        }

        private fun parseHexStringOrThrow(str: String): Int {
            if (!str.startsWith("#")) {
                throw IllegalArgumentException("Hex color must start with #")
            }
            if (str.length != 7) {
                throw IllegalArgumentException("Hex color must be 7 characters long")
            }
            return str.removePrefix("#").toInt(16)
        }

        /**
         * Create the link embeds for the message
         *
         * It may be an empty list depending on the link type
         */
        fun createLinkEmbeds(): List<DiscordAPI.Embed> {
            with(parameters) {
                if (LinkType.valueOf(style.get().link.get()) != LinkType.EMBED) {
                    // Return empty list as the link type is not EMBED
                    return listOf()
                }

                val embeds = publishResults.files.map {
                    PublishResult.fromJson(it.readText())
                }.map {
                    DiscordAPI.Embed(
                        title = it.title,
                        url = it.link,
                        color = it.brandColor,
                    )
                }.toList()

                // Find any embeds with duplicate URLs and throw and error if there are any.
                for (embed in embeds) {
                    val count = embeds.count { it.url == embed.url }
                    if (count > 1) {
                        throw IllegalStateException("Duplicate embed URL: ${embed.url} for ${embed.title}")
                    }
                }

                return embeds
            }
        }

        /**
         * Create the link buttons for the message
         *
         * It may be an empty list depending on the link type
         */
        fun createComponents(): List<DiscordAPI.ActionRow> {
            with(parameters) {
                if (LinkType.valueOf(style.get().link.get()) != LinkType.BUTTON) {
                    // Return empty list as the link type is not BUTTON
                    return listOf()
                }

                val components = publishResults.files.map {
                    PublishResult.fromJson(it.readText())
                }.map {
                    // Create URL button for the message
                    DiscordAPI.ButtonComponent(
                        // Button label, the title for the publisher
                        label = it.title,
                        // The URL for the mod page
                        url = it.link,
                    )
                }.toList()

                // Find any embeds with duplicate URLs and throw and error if there are any.
                for (component in components) {
                    val count = components.count { it.url == component.url }
                    if (count > 1) {
                        throw IllegalStateException("Duplicate component URL: ${component.url} for ${component.label}")
                    }
                }

                // An action row is a row of components,
                // it can have up to 5 of them
                return components.chunked(5).map {
                    DiscordAPI.ActionRow(
                        components = it,
                    )
                }
            }
        }

        /**
         * Create the body of the message
         *
         * This is used to inject content if needed
         */
        fun createMessageBody(): String {
            with(parameters) {
                var content = content.get()

                if (LinkType.valueOf(style.get().link.get()) == LinkType.INLINE) {
                    publishResults.files.map {
                        PublishResult.fromJson(it.readText())
                    }.forEach {
                        // Append the links to the end of the message
                        // !!! The current implementation does not support emotes for inline links !!!
                        content += "\n[${it.title}](${it.link})"
                    }
                }

                return content
            }
        }
    }
}
