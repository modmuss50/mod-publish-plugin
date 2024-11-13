package me.modmuss50.mpp.platforms.github

import me.modmuss50.mpp.GithubPublishResult
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
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import javax.inject.Inject
import kotlin.random.Random

interface GithubOptions : PlatformOptions, PlatformOptionsInternal<GithubOptions> {
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
    val commitish: Property<String>

    @get:Input
    val tagName: Property<String>

    @get:Input
    @get:Optional
    val apiEndpoint: Property<String>

    @get:Input
    val allowEmptyFiles: Property<Boolean>

    @get:InputFile
    @get:Optional
    @get:Internal
    val releaseResult: RegularFileProperty

    override fun setInternalDefaults() {
        tagName.convention(version)
        allowEmptyFiles.convention(false)
    }

    fun from(other: GithubOptions) {
        super.from(other)
        repository.convention(other.repository)
        commitish.convention(other.commitish)
        tagName.convention(other.tagName)
        apiEndpoint.convention(other.apiEndpoint)
        allowEmptyFiles.convention(other.allowEmptyFiles)
        releaseResult.convention(other.releaseResult)
    }

    fun from(other: Provider<GithubOptions>) {
        from(other.get())
    }

    fun from(other: Provider<GithubOptions>, publishOptions: Provider<PublishOptions>) {
        from(other)
        from(publishOptions.get())
    }

    /**
     * Publish to an existing release, created by another task.
     */
    fun parent(task: TaskProvider<Task>) {
        val publishTask = task.map { it as PublishModTask }
        releaseResult.set(publishTask.flatMap { it.result })

        val options = publishTask.map { it.platform as GithubOptions }
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
}

abstract class Github @Inject constructor(name: String) : Platform(name), GithubOptions {
    override fun publish(context: PublishContext) {
        val files = additionalFiles.files.toMutableList()

        if (file.isPresent) {
            files.add(file.get().asFile)
        }

        if (files.isEmpty() && !allowEmptyFiles.get()) {
            throw IllegalStateException("No files to upload to GitHub.")
        }

        context.submit(UploadWorkAction::class) {
            it.from(this)
        }
    }

    override fun dryRunPublishResult(): PublishResult {
        return GithubPublishResult(
            repository = repository.get(),
            releaseId = 0,
            url = "https://github.com/modmuss50/mod-publish-plugin/dry-run?random=${Random.nextInt(0, 1000000)}",
            title = announcementTitle.getOrElse("Download from GitHub"),
        )
    }

    override fun printDryRunInfo(logger: Logger) {
    }

    interface UploadParams : PublishWorkParameters, GithubOptions

    abstract class UploadWorkAction : PublishWorkAction<UploadParams> {
        // TODO: Maybe look at moving away from using a large library for this.
        override fun publish(): PublishResult {
            with(parameters) {
                val repo = connect().getRepository(repository.get())
                val release = getOrCreateRelease(repo)

                val files = additionalFiles.files.toMutableList()

                if (file.isPresent) {
                    files.add(file.get().asFile)
                }

                for (file in files) {
                    release.uploadAsset(file, "application/java-archive")
                }

                return GithubPublishResult(
                    repository = repository.get(),
                    releaseId = release.id,
                    url = release.htmlUrl.toString(),
                    title = announcementTitle.getOrElse("Download from GitHub"),
                )
            }
        }

        private fun getOrCreateRelease(repo: GHRepository): GHRelease {
            with(parameters) {
                if (releaseResult.isPresent) {
                    val result = PublishResult.fromJson(releaseResult.get().asFile.readText()) as GithubPublishResult
                    return repo.getRelease(result.releaseId)
                }

                return with(GHReleaseBuilder(repo, tagName.get())) {
                    name(displayName.get())
                    body(changelog.get())
                    prerelease(type.get() != ReleaseType.STABLE)
                    commitish(commitish.get())
                }.create()
            }
        }

        private fun connect(): GitHub {
            val accessToken = parameters.accessToken.get()
            val endpoint = parameters.apiEndpoint.orNull

            if (endpoint != null) {
                return GitHub.connectUsingOAuth(endpoint, accessToken)
            }

            return GitHub.connectUsingOAuth(accessToken)
        }
    }
}
