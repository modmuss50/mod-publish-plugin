package me.modmuss50.mpp.platforms.discord

import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.modPublishExtension
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
    @get:Optional
    val thumbnailUrl: Property<String>

    @get:Input
    @get:Optional
    val color: Property<Any>

    @get:Input
    @get:Optional
    val useComponents: Property<String>

    @get:Input
    val content: Property<String>

    fun from(other: DiscordWebhookOptions) {
        webhookUrl.set(other.webhookUrl)
        dryRunWebhookUrl.set(other.dryRunWebhookUrl)
        username.set(other.username)
        avatarUrl.set(other.avatarUrl)
        thumbnailUrl.set(other.thumbnailUrl)
        color.set(other.color)
        useComponents.set(other.useComponents)
        content.set(other.content)
    }
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

    @TaskAction
    fun announce() {
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(DiscordWorkAction::class.java) {
            it.from(this)
            it.publishResults.setFrom(publishResults)
            it.dryRun.set(dryRun)
        }
    }

    interface DiscordWorkParameters : WorkParameters, DiscordWebhookOptions {
        val publishResults: ConfigurableFileCollection

        val dryRun: Property<Boolean>
    }

    abstract class DiscordWorkAction : WorkAction<DiscordWorkParameters> {
        override fun execute() {
            with(parameters) {
                val componentType = when (useComponents.orNull?.lowercase()) {
                    "message" -> ComponentMessageType.MESSAGE
                    "embed" -> ComponentMessageType.EMBED
                    else -> ComponentMessageType.MESSAGE
                }
                val useComponents = useComponents.isPresent

                if (dryRun.get() && !dryRunWebhookUrl.isPresent) {
                    // Don't announce if we're dry running and don't have a dry run webhook URL.
                    return
                }

                val embeds = if (useComponents) {
                    if (componentType == ComponentMessageType.EMBED) {
                        listOf(
                            DiscordAPI.Embed(
                                thumbnail = if (thumbnailUrl.isPresent) {
                                    DiscordAPI.EmbedThumbnail(
                                        url = thumbnailUrl.get(),
                                    )
                                } else {
                                    null
                                },
                                description = content.get(),
                                color = when (color.orNull) {
                                    "modrinth" -> 0x1BD96A
                                    "github" -> 0xF6F0FC
                                    "curseforge" -> 0xF16436
                                    is Int -> color.get() as Int
                                    is String -> {
                                        val hex = color.get() as String
                                        println(hex.removePrefix("#").toInt(16))
                                        hex.removePrefix("#").toInt(16)
                                    }
                                    else -> null
                                },
                            ),
                        )
                    } else {
                        listOf()
                    }
                } else {
                    publishResults.files.map {
                        PublishResult.fromJson(it.readText())
                    }.map {
                        DiscordAPI.Embed(
                            title = it.title,
                            url = it.link,
                            color = it.brandColor,
                        )
                    }.toList()
                }

                val components = if (useComponents) {
                    publishResults.files.map {
                        PublishResult.fromJson(it.readText())
                    }.map {
                        DiscordAPI.ButtonComponent(
                            label = it.title,
                            url = it.link,
                        )
                    }.toList()
                } else {
                    listOf()
                }

                // Find any embeds with duplicate URLs and throw and error if there are any.
                if (!useComponents) {
                    for (embed in embeds) {
                        val count = embeds.count { it.url == embed.url }
                        if (count > 1) {
                            throw IllegalStateException("Duplicate embed URL: ${embed.url} for ${embed.title}")
                        }
                    }
                } else {
                    for (component in components) {
                        val count = embeds.count { it.url == component.url }
                        if (count > 1) {
                            throw IllegalStateException("Duplicate component URL: ${component.url} for ${component.label}")
                        }
                    }
                }

                val url = if (dryRun.get()) dryRunWebhookUrl else webhookUrl

                if (!useComponents) {
                    var firstRequest = true
                    embeds.chunked(10).forEach { chunk ->
                        DiscordAPI.executeWebhook(
                            url.get(),
                            DiscordAPI.Webhook(
                                username = username.get(),
                                content = if (firstRequest) content.get() else null,
                                avatarUrl = avatarUrl.orNull,
                                embeds = chunk,
                            ),
                        )

                        firstRequest = false
                    }
                } else {
                    DiscordAPI.executeWebhook(
                        url.get(),
                        DiscordAPI.Webhook(
                            content = content.get(),
                            username = username.get(),
                            avatarUrl = avatarUrl.orNull,
                            embeds = embeds,
                            components = components.chunked(5).map {
                                DiscordAPI.ActionRow(
                                    components = it,
                                )
                            },
                        ),
                    )
                }
            }
        }
    }

    enum class ComponentMessageType {
        MESSAGE,
        EMBED,
    }
}
