package me.modmuss50.mpp.platforms.gitlab

import me.modmuss50.mpp.GitlabPublishResult
import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.PlatformOptionsInternal
import me.modmuss50.mpp.PublishContext
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.PublishOptions
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.PublishWorkAction
import me.modmuss50.mpp.PublishWorkParameters
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
import javax.inject.Inject
import kotlin.random.Random

interface GitlabOptions : PlatformOptions, PlatformOptionsInternal<GitlabOptions> {

    @get:InputFile
    @get:Optional
    override val file: RegularFileProperty

    /**
     * GitLab uses project IDs as opposed to repository links.
     */
    @get:Input
    val projectId: Property<Long>

    @get:Input
    val tagName: Property<String>

    @get:Input
    val commitish: Property<String>

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

    fun from(
        other: GitlabOptions,
    ) {
        super.from(other)
        projectId.convention(other.projectId)
        tagName.convention(other.tagName)
        commitish.convention(other.commitish)
        apiEndpoint.convention(other.apiEndpoint)
        allowEmptyFiles.convention(other.allowEmptyFiles)
        releaseResult.convention(other.releaseResult)
    }

    fun from(
        other: Provider<GitlabOptions>,
    ) {
        from(other.get())
    }

    fun from(
        other: Provider<GitlabOptions>,
        publishOptions: Provider<PublishOptions>,
    ) {
        from(other)
        from(publishOptions.get())
    }

    /**
     * Publish to an existing release, created by another task.
     */
    fun parent(
        task: TaskProvider<Task>,
    ) {
        val publishTask = task.map { it as PublishModTask }
        releaseResult.set(publishTask.flatMap { it.result })

        val options = publishTask.map { it.platform as GitlabOptions }
        version.set(options.flatMap { it.version })
        version.finalizeValue()
        changelog.set(options.flatMap { it.changelog })
        changelog.finalizeValue()
        type.set(options.flatMap { it.type })
        type.finalizeValue()
        displayName.set(options.flatMap { it.displayName })
        displayName.finalizeValue()
        projectId.set(options.flatMap { it.projectId })
        projectId.finalizeValue()
        commitish.set(options.flatMap { it.commitish })
        commitish.finalizeValue()
        tagName.set(options.flatMap { it.tagName })
        tagName.finalizeValue()
    }
}

abstract class Gitlab @Inject constructor(name: String) : Platform(name), GitlabOptions {
    override fun publish(
        context: PublishContext,
    ) {
        val files = additionalFiles.files.toMutableList()
        if (file.isPresent) files.add(file.get().asFile)

        if (files.isEmpty() && !allowEmptyFiles.get()) {
            throw IllegalStateException("No files to upload to GitLab.")
        }

        context.submit(UploadWorkAction::class) {
            it.from(this)
        }
    }

    override fun dryRunPublishResult(): PublishResult {
        return GitlabPublishResult(
            projectId = projectId.get(),
            tagName = tagName.get(),
            url = "https://gitlab.com/dry-run?random=${Random.nextInt(0, 1000000)}",
            title = announcementTitle.getOrElse("Download from GitLab"),
        )
    }

    override fun printDryRunInfo(logger: Logger) {}

    interface UploadParams : PublishWorkParameters, GitlabOptions

    abstract class UploadWorkAction : PublishWorkAction<UploadParams> {
        override fun publish(): PublishResult {
            with(parameters) {
                val api = GitlabApi(
                    accessToken = accessToken.get(),
                    apiEndpoint = apiEndpoint.orNull ?: "https://gitlab.com/api/v4",
                )

                getOrCreateRelease(api)

                val files = additionalFiles.files.toMutableList()
                if (file.isPresent) {
                    files.add(file.get().asFile)
                }

                if (files.isEmpty() && !allowEmptyFiles.get()) {
                    throw IllegalStateException("No files to upload to GitLab.")
                }

                val duplicates = files.groupingBy { it.name }
                    .eachCount()
                    .filter { it.value > 1 }

                if (duplicates.isNotEmpty()) {
                    val duplicateNames = duplicates.keys.joinToString(", ")
                    throw IllegalStateException(
                        "GitLab file names must be unique within a release, found duplicates: $duplicateNames",
                    )
                }

                for (f in files) {
                    val uploaded = api.uploadAsset(projectId.get(), f)
                    api.addAssetToRelease(projectId.get(), tagName.get(), uploaded)
                }

                return GitlabPublishResult(
                    projectId = projectId.get(),
                    tagName = tagName.get(),
                    url = "https://gitlab.com/projects/${projectId.get()}/releases/${tagName.get()}",
                    title = announcementTitle.getOrElse("Download from GitLab"),
                )
            }
        }

        data class ReleaseResult(val release: GitlabApi.Release, val created: Boolean)

        private fun getOrCreateRelease(
            api: GitlabApi,
        ): ReleaseResult {
            with(parameters) {
                if (releaseResult.isPresent) {
                    val result = PublishResult.fromJson(
                        releaseResult.get().asFile.readText(),
                    ) as GitlabPublishResult

                    return ReleaseResult(
                        api.getRelease(projectId.get(), result.tagName),
                        false,
                    )
                }

                // This is a little more complicated than GitHub due to how GitLab treats tags as unique per release
                return try {
                    val release = api.createRelease(
                        projectId.get(),
                        GitlabApi.CreateReleaseRequest(
                            name = displayName.get(),
                            tagName = tagName.get(),
                            description = changelog.get(),
                            ref = commitish.get(),
                        ),
                    )
                    ReleaseResult(release, true)
                } catch (e: Exception) {
                    if (true == e.message?.contains("409") || true == e.message?.contains("already exists")) {
                        val existing = api.getRelease(projectId.get(), tagName.get())
                        ReleaseResult(existing, false)
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
