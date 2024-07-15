package me.modmuss50.mpp.platforms.discord

import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.modPublishExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
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

@DisableCachingByDefault(because = "Publish webhook each time")
abstract class DiscordWebhookTask : DefaultTask(), DiscordWebhookOptions {
    @get:InputFiles
    abstract val publishResults: ConfigurableFileCollection

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    init {
        group = "publishing"
        username.convention("Mod Publish Plugin")
        content.convention(project.modPublishExtension.changelog)

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
    fun setPlatforms(vararg tasks: TaskProvider<Task>) {
        publishResults.setFrom(
            tasks
                .map { task -> task.map { it as PublishModTask } }
                .map { task -> task.flatMap { it.result } },
        )
    }

    /**
     * Set the platforms to announce, by passing in publish tasks.
     */
    fun setPlatforms(tasks: TaskCollection<Task>) {
        publishResults.setFrom(
            tasks
                .map { it as PublishModTask }
                .map { it.result },
        )
    }

    @TaskAction
    fun announce() {
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(DiscordWorkAction::class.java) {
            it.from(this)
            it.publishResults.setFrom(publishResults)
            it.dryRun.set(project.modPublishExtension.dryRun)
        }
    }

    interface DiscordWorkParameters : WorkParameters, DiscordWebhookOptions {
        val publishResults: ConfigurableFileCollection

        val dryRun: Property<Boolean>
    }

    abstract class DiscordWorkAction : WorkAction<DiscordWorkParameters> {
        override fun execute() {
            with(parameters) {
                if (dryRun.get() && !dryRunWebhookUrl.isPresent) {
                    // Don't announce if we're dry running and don't have a dry run webhook URL.
                    return
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

                val url = if (dryRun.get()) dryRunWebhookUrl else webhookUrl

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
            }
        }
    }
}
