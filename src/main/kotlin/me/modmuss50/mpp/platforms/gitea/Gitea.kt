package me.modmuss50.mpp.platforms.gitea

import me.modmuss50.mpp.GiteaPublishResult
import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.PlatformOptionsInternal
import me.modmuss50.mpp.PublishContext
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.PublishOptions
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.PublishWorkAction
import me.modmuss50.mpp.PublishWorkParameters
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.gitea.GiteaOptions.HostType
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

interface GiteaOptions : PlatformOptions, PlatformOptionsInternal<GiteaOptions> {
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
    val apiEndpoint: Property<String>

    @get:Input
    val allowEmptyFiles: Property<Boolean>

    @get:InputFile
    @get:Optional
    @get:Internal
    val releaseResult: RegularFileProperty

    /**
     * Specifies the display name for the custom host. For example, Codeberg.
     */
    @get:Input
    val hostDisplayName: Property<String>

    /**
     * Specifies a custom brand color for Discord embeds. Useful for specific hosts.
     */
    @get:Input
    val hostBrandColor: Property<Int>

    override fun setInternalDefaults() {
        tagName.convention(version)
        allowEmptyFiles.convention(false)
    }

    fun hostUrl(task: Provider<String>) {
        apiEndpoint.convention(task.get() + "/api/v1")
    }

    fun from(other: GiteaOptions) {
        super.from(other)
        repository.convention(other.repository)
        commitish.convention(other.commitish)
        tagName.convention(other.tagName)
        apiEndpoint.convention(other.apiEndpoint)
        allowEmptyFiles.convention(other.allowEmptyFiles)
        releaseResult.convention(other.releaseResult)
    }

    fun from(other: Provider<GiteaOptions>) {
        from(other.get())
    }

    fun from(other: Provider<GiteaOptions>, publishOptions: Provider<PublishOptions>) {
        from(other)
        from(publishOptions.get())
    }

    /**
     * Publish to an existing release, created by another task.
     */
    fun parent(task: TaskProvider<Task>) {
        val publishTask = task.map { it as PublishModTask }
        releaseResult.set(publishTask.flatMap { it.result })

        val options = publishTask.map { it.platform as GiteaOptions }
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
    }

    enum class HostType constructor(val friendlyString: String, val defaultBrandColor: Int) {
        GITEA("Gitea", 0x1d8f4a),

        FORGEJO("Forgejo", 0xff5500);
    }
}

interface HostTyped {
    @get:InputFile
    @get:Internal
    val hostType: Property<HostType>
}

abstract class Gitea @Inject constructor(name: String) : Platform(name), GiteaOptions, HostTyped {
    override fun publish(context: PublishContext) {
        val files = additionalFiles.files.toMutableList()

        if (file.isPresent) {
            files.add(file.get().asFile)
        }

        if (files.isEmpty() && !allowEmptyFiles.get()) {
            throw IllegalStateException("No files to upload to ${capitalizedName()}.")
        }

        context.submit(UploadWorkAction::class) {
            it.from(this)
        }
    }

    override fun dryRunPublishResult(): PublishResult {
        val hostDisplayName = hostDisplayName.getOrElse(hostType.get().friendlyString)
        val brandColor = hostBrandColor.getOrElse(hostType.get().defaultBrandColor)

        return GiteaPublishResult(
            repository = repository.get(),
            releaseId = 0,
            url = "https://github.com/modmuss50/mod-publish-plugin/dry-run?random=${Random.nextInt(0, 1000000)}",
            title = announcementTitle.getOrElse("Download from $hostDisplayName"),
            brandColor = brandColor
        )
    }

    override fun printDryRunInfo(logger: Logger) {
    }

    fun capitalizedName(): String {
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    interface UploadParams : PublishWorkParameters, GiteaOptions, HostTyped

    abstract class UploadWorkAction : PublishWorkAction<UploadParams> {
        override fun publish(): PublishResult {
            with(parameters) {
                val hostDisplayName = hostDisplayName.getOrElse(hostType.get().friendlyString)
                val brandColor = hostBrandColor.getOrElse(hostType.get().defaultBrandColor)

                val api = GiteaApi(accessToken.get(), apiEndpoint.get(), repository.get())
                val (release, created) = getOrCreateRelease(api)

                val files = additionalFiles.files.toMutableList()

                if (file.isPresent) {
                    files.add(file.get().asFile)
                }

                val noneUnique = files.groupingBy { it.name }.eachCount().filter { it.value > 1 }
                if (noneUnique.isNotEmpty()) {
                    val noneUniqueNames = noneUnique.keys.joinToString(", ")
                    throw IllegalStateException("$hostDisplayName file names must be unique within a release, found duplicates: $noneUniqueNames")
                }

                for (file in files) {
                    api.uploadAsset(release,  displayName.get(), file)
                }

                if (created) {
                    // Publish the release after all assets are uploaded.
                    api.publishRelease(release)
                }

                return GiteaPublishResult(
                    repository = repository.get(),
                    releaseId = release.id,
                    url = release.htmlUrl,
                    title = announcementTitle.getOrElse("Download from $hostDisplayName"),
                    brandColor = brandColor
                )
            }
        }

        data class ReleaseResult(val release: GiteaApi.Release, val created: Boolean)

        private fun getOrCreateRelease(api: GiteaApi): ReleaseResult {
            with(parameters) {
                if (releaseResult.isPresent) {
                    val result = PublishResult.fromJson(releaseResult.get().asFile.readText()) as GiteaPublishResult
                    return ReleaseResult(api.getRelease(result.releaseId), false)
                }


                val metadata = GiteaApi.CreateRelease(
                    body = changelog.orNull,
                    draft = true,
                    name = displayName.get(),
                    prerelease = type.get() != ReleaseType.STABLE,
                    tagName = tagName.get(),
                    targetCommitish = commitish.get()
                )

                return ReleaseResult(api.createRelease(metadata), true)
            }
        }
    }
}
