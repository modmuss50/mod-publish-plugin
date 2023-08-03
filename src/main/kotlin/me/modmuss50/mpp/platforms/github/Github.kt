package me.modmuss50.mpp.platforms.github

import me.modmuss50.mpp.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
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
    @get:Optional
    val apiEndpoint: Property<String>

    override fun setInternalDefaults() {
    }

    fun from(other: GithubOptions) {
        super.from(other)
        repository.set(other.repository)
        commitish.set(other.commitish)
        apiEndpoint.set(other.apiEndpoint)
    }
}

abstract class Github @Inject constructor(name: String) : Platform(name), GithubOptions {
    override fun publish(queue: WorkQueue) {
        queue.submit(UploadWorkAction::class.java) {
            it.from(this)
        }
    }

    interface UploadParams : WorkParameters, GithubOptions

    abstract class UploadWorkAction : WorkAction<UploadParams> {
        // TODO: Maybe look at moving away from using a large library for this.
        override fun execute() {
            with(parameters) {
                val mainFile = file.get().asFile

                val repo = connect().getRepository(repository.get())
                val release = with(GHReleaseBuilder(repo, version.get())) {
                    name(displayName.get())
                    body(changelog.get())
                    prerelease(type.get() != ReleaseType.STABLE)
                    commitish(commitish.get())
                }.create()

                release.uploadAsset(mainFile, "application/java-archive")

                for (additionalFile in additionalFiles.files) {
                    release.uploadAsset(additionalFile, "application/java-archive")
                }
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
