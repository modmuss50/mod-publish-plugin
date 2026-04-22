package me.modmuss50.mpp.platforms.gitea.base

import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.PlatformOptionsInternal
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.PublishOptions
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider
import java.net.URI

interface GiteaCompatibleOptions :
    PlatformOptions,
    PlatformOptionsInternal<GiteaCompatibleOptions> {

    @get:InputFile
    @get:Optional
    override val file: RegularFileProperty

    /**
     * "owner/repo"
     */
    @get:Input
    val repository: Property<String>

    /**
     * Specifies the commitish value that determines where the Git tag is created from. Can be any branch or commit SHA.
     */
    @get:Input
    @get:Optional
    val commitish: Property<String>

    @get:Input
    val tagName: Property<String>

    @get:Input
    @get:Optional
    val apiEndpoint: Property<String>

    /**
     * Specifies the display name for the custom host. For example, Codeberg.
     */
    @get:Input
    @get:Optional
    val hostDisplayName: Property<String>

    /**
     * Specifies a custom brand color for Discord embeds. Useful for specific hosts.
     */
    @get:Input
    @get:Optional
    val hostBrandColor: Property<Int>

    @get:Input
    val allowEmptyFiles: Property<Boolean>

    @get:InputFile
    @get:Optional
    val releaseResult: RegularFileProperty

    @get:Input
    @get:Optional
    val hostType: Property<GiteaCompatiblePlatform>

    override fun setInternalDefaults() {
        tagName.convention(version)
        allowEmptyFiles.convention(false)
    }

    fun host(uri: URI) {
        apiEndpoint.convention("$uri/api/v1")
    }

    fun from(other: GiteaCompatibleOptions) {
        super.from(other)
        repository.convention(other.repository)
        commitish.convention(other.commitish)
        tagName.convention(other.tagName)
        apiEndpoint.convention(other.apiEndpoint)
        allowEmptyFiles.convention(other.allowEmptyFiles)
        releaseResult.convention(other.releaseResult)
        hostType.convention(other.hostType)
    }

    fun from(other: Provider<GiteaCompatibleOptions>) {
        from(other.get())
    }

    fun from(
        other: Provider<GiteaCompatibleOptions>,
        publishOptions: Provider<PublishOptions>,
    ) {
        from(other)
        from(publishOptions.get())
    }

    /**
     * Publish to an existing release, created by another task.
     */
    fun parent(task: TaskProvider<Task>) {
        val publishTask = task.map { it as PublishModTask }
        releaseResult.set(publishTask.flatMap { it.result })

        val options = publishTask.map { it.platform as GiteaCompatibleOptions }
        if (options.get().hostType.get() != hostType.get()) { // May not be necessary, but should reduce confusion.
            throw IllegalStateException(
                "Unable to make a ${options.get().hostType.get().friendlyString} instance a parent of a ${hostType.get().friendlyString} instance",
            )
        }

        version.set(options.flatMap { it.version })
        version.finalizeValue()
        changelog.set(options.flatMap { it.changelog })
        changelog.finalizeValue()
        type.set(options.flatMap { it.type })
        type.finalizeValue()
        displayName.set(options.flatMap { it.displayName })
        displayName.finalizeValue()
        repository.set(options.flatMap { it.repository })
        repository.finalizeValue()
        commitish.set(options.flatMap { it.commitish })
        commitish.finalizeValue()
        tagName.set(options.flatMap { it.tagName })
        tagName.finalizeValue()

        // Include this here to make sure that different hosts may resolve correctly when parenting. This is not included in other platforms.
        apiEndpoint.set(options.flatMap { it.apiEndpoint })
        apiEndpoint.finalizeValue()
    }
}
