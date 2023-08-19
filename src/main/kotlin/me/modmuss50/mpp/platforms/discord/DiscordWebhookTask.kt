package me.modmuss50.mpp.platforms.discord

import me.modmuss50.mpp.CurseForgePublishResult
import me.modmuss50.mpp.GithubPublishResult
import me.modmuss50.mpp.ModrinthPublishResult
import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.modPublishExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

interface DiscordWebhookOptions {
    @get:Input
    val webhookUrl: Property<String>

    @get:Input
    val username: Property<String>

    @get:Input
    @get:Optional
    val avatarUrl: Property<String>

    @get:Input
    val content: Property<String>

    fun from(other: DiscordWebhookOptions) {
        webhookUrl.set(other.webhookUrl)
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

    fun setPlatforms(vararg platforms: Platform) {
        publishResults.setFrom(
            platforms
                .map { platform -> project.tasks.getByName(platform.taskName) as PublishModTask }
                .map { task -> task.result },
        )
    }

    @TaskAction
    fun announce() {
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(DiscordWorkAction::class.java) {
            it.from(this)
            it.publishResults.setFrom(publishResults)
        }
    }

    interface DiscordWorkParameters : WorkParameters, DiscordWebhookOptions {
        val publishResults: ConfigurableFileCollection
    }

    abstract class DiscordWorkAction : WorkAction<DiscordWorkParameters> {
        override fun execute() {
            with(parameters) {
                val embeds = publishResults.files.map {
                    PublishResult.fromJson(it.readText())
                }.map {
                    DiscordAPI.Embed(
                        title = "Download from ${displayName(it)}",
                        url = it.link,
                        color = brandColor(it),
                    )
                }.toList()

                DiscordAPI.executeWebhook(
                    webhookUrl.get(),
                    DiscordAPI.Webhook(
                        username = username.get(),
                        content = content.get(),
                        avatarUrl = avatarUrl.orNull,
                        embeds = embeds,
                    ),
                )
            }
        }

        private fun displayName(result: PublishResult): String {
            return when (result) {
                is CurseForgePublishResult -> "CurseForge"
                is GithubPublishResult -> "GitHub"
                is ModrinthPublishResult -> "Modrinth"
            }
        }

        private fun brandColor(result: PublishResult): Int {
            return when (result) {
                is CurseForgePublishResult -> 0xF16436
                is GithubPublishResult -> 0xF6F0FC
                is ModrinthPublishResult -> 0x1BD96A
            }
        }
    }
}
