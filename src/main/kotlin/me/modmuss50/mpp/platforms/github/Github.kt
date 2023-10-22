package me.modmuss50.mpp.platforms.github

import me.modmuss50.mpp.GithubPublishResult
import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.PlatformOptionsInternal
import me.modmuss50.mpp.PublishContext
import me.modmuss50.mpp.PublishOptions
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.PublishWorkAction
import me.modmuss50.mpp.PublishWorkParameters
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GitHub
import javax.inject.Inject

interface GithubOptions : PlatformOptions, PlatformOptionsInternal<GithubOptions> {
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

    override fun setInternalDefaults() {
        tagName.convention(version)
    }

    fun from(other: GithubOptions) {
        super.from(other)
        repository.set(other.repository)
        commitish.set(other.commitish)
        apiEndpoint.set(other.apiEndpoint)
        tagName.set(other.tagName)
    }

    fun from(other: Provider<GithubOptions>) {
        from(other.get())
    }

    fun from(other: Provider<GithubOptions>, publishOptions: Provider<PublishOptions>) {
        from(other)
        from(publishOptions.get())
    }
}

abstract class Github @Inject constructor(name: String) : Platform(name), GithubOptions {
    override fun publish(context: PublishContext) {
        context.submit(UploadWorkAction::class) {
            it.from(this)
        }
    }

    interface UploadParams : PublishWorkParameters, GithubOptions

    abstract class UploadWorkAction : PublishWorkAction<UploadParams> {
        // TODO: Maybe look at moving away from using a large library for this.
        override fun publish(): PublishResult {
            with(parameters) {
                val mainFile = file.get().asFile

                val repo = connect().getRepository(repository.get())
                val release = with(GHReleaseBuilder(repo, tagName.get())) {
                    name(displayName.get())
                    body(changelog.get())
                    prerelease(type.get() != ReleaseType.STABLE)
                    commitish(commitish.get())
                }.create()

                release.uploadAsset(mainFile, "application/java-archive")

                for (additionalFile in additionalFiles.files) {
                    release.uploadAsset(additionalFile, "application/java-archive")
                }

                return GithubPublishResult(
                    repository = repository.get(),
                    releaseId = release.id,
                    url = release.htmlUrl.toString(),
                    title = announcementTitle.getOrElse("Download from GitHub"),
                )
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
