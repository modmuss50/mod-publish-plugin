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
import org.gradle.api.tasks.Optional
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

    fun from(other: DiscordWebhookOptions) {
        webhookUrl.set(other.webhookUrl)
        dryRunWebhookUrl.set(other.dryRunWebhookUrl)
        username.set(other.username)
        avatarUrl.set(other.avatarUrl)
        content.set(other.content)
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class MessageStyle internal constructor() : java.io.Serializable {
    var type: MessageStyles = MessageStyles.CLASSIC
    var thumbnailUrl: String? = null
    var color: Any? = null
    var link: LinkType = LinkType.EMBED

    fun from(other: MessageStyle) {
        type = other.type
        thumbnailUrl = other.thumbnailUrl
        color = other.color
        link = other.link
    }
}

enum class LinkType {
    EMBED,
    BUTTON,
}

enum class MessageStyles {
    MODERN,
    CLASSIC,
}

@DisableCachingByDefault(because = "Publish webhook each time")
abstract class DiscordWebhookTask : DefaultTask(), DiscordWebhookOptions {
    @get:ApiStatus.Internal
    @get:Input
    abstract val dryRun: Property<Boolean>

    @get:InputFiles
    abstract val publishResults: ConfigurableFileCollection

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @JvmField
    @Suppress("MemberVisibilityCanBePrivate")
    protected val style: MessageStyle = MessageStyle()

    init {
        group = "publishing"
        username.convention("Mod Publish Plugin")
        content.convention(project.modPublishExtension.changelog)

        dryRun.set(project.modPublishExtension.dryRun)
        dryRun.finalizeValue()

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

    fun style(style: Action<MessageStyle>) {
        style.execute(this.style)
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

        val style: Property<MessageStyle>
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

                if (style.get().link == LinkType.BUTTON) {
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

                // Split the embeds across multiple messages if needed
                val embedChunks = embeds.chunked(10)
                embedChunks.forEachIndexed { index, chunk ->
                    DiscordAPI.executeWebhook(
                        url.get(),
                        DiscordAPI.Webhook(
                            username = username.get(),
                            content = if (index == 0) createClassicMessage() else null,
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
                            content = if (firstRequest) createClassicMessage() else null,
                            username = username.get(),
                            avatarUrl = avatarUrl.orNull,
                            components = components.next(),
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
         * Depending on the message style, only the links may be present,
         * or it may be empty
         */
        fun createEmbeds(): List<DiscordAPI.Embed> {
            with(parameters) {
                return when (style.get().type) {
                    MessageStyles.CLASSIC -> createLinkEmbeds()
                    // Get the link embeds and the modern embed
                    MessageStyles.MODERN -> listOf(createModernEmbed()) + createLinkEmbeds()
                }
            }
        }

        /**
         * Create the message body for the classic style
         *
         * It may be null depending on the configured style
         */
        fun createClassicMessage(): String? {
            with(parameters) {
                if (style.get().type != MessageStyles.CLASSIC) {
                    return null
                }

                return content.get()
            }
        }

        /**
         * Create the message embed for the modern style
         *
         * It will never be null
         */
        fun createModernEmbed(): DiscordAPI.Embed {
            with(parameters) {
                val style: MessageStyle = style.get()
                return DiscordAPI.Embed(
                    thumbnail = when (style.thumbnailUrl) {
                        is String -> DiscordAPI.EmbedThumbnail(url = style.thumbnailUrl!!)
                        else -> null
                    },
                    description = content.get(),
                    color = when (style.color) {
                        "modrinth" -> 0x1BD96A
                        "github" -> 0xF6F0FC
                        "curseforge" -> 0xF16436
                        is Int -> style.color as Int
                        is String -> {
                            val hex = style.color as String
                            hex.removePrefix("#").toInt(16)
                        }

                        else -> null
                    },
                )
            }
        }

        /**
         * Create the link embeds for the message
         *
         * It may be an empty list depending on the configured style
         */
        fun createLinkEmbeds(): List<DiscordAPI.Embed> {
            with(parameters) {
                if (style.get().link != LinkType.EMBED) {
                    // Return empty list as there is no link embed
                    // Doing this helps keeping createEmbeds cleaner
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
         * It may be an empty list depending on the configured style
         */
        fun createComponents(): List<DiscordAPI.ActionRow> {
            with(parameters) {
                if (style.get().link != LinkType.BUTTON) {
                    // Return empty list as there is no button
                    // Doing this helps keeping createEmbeds cleaner
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
    }
}
