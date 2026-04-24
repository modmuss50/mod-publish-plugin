package me.modmuss50.mpp.platforms.gitea

import me.modmuss50.mpp.GiteaCompatiblePublishResult
import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PublishContext
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.PublishWorkAction
import me.modmuss50.mpp.PublishWorkParameters
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.gitea.base.GiteaCompatibleApi
import me.modmuss50.mpp.platforms.gitea.base.GiteaCompatibleOptions
import me.modmuss50.mpp.platforms.gitea.base.GiteaCompatiblePlatform
import org.gradle.api.logging.Logger
import javax.inject.Inject
import kotlin.random.Random

abstract class Codeberg
@Inject
constructor(
    name: String,
) : Platform(name),
    GiteaCompatibleOptions {
    override fun publish(context: PublishContext) {
        val files = additionalFiles.files.toMutableList()

        if (file.isPresent) {
            files.add(file.get().asFile)
        }

        if (files.isEmpty() && !allowEmptyFiles.get()) {
            throw IllegalStateException("No files to upload to ${GiteaCompatiblePlatform.CODEBERG.friendlyString}.")
        }

        context.submit(UploadWorkAction::class) {
            it.from(this)
        }
    }

    override fun dryRunPublishResult(): PublishResult =
        GiteaCompatiblePublishResult(
            repository = repository.get(),
            releaseId = 0,
            url = "https://github.com/modmuss50/mod-publish-plugin/dry-run?random=${Random.nextInt(0, 1000000)}",
            title = announcementTitle.getOrElse("Download from ${GiteaCompatiblePlatform.CODEBERG.friendlyString}"),
            brandColor = GiteaCompatiblePlatform.CODEBERG.defaultBrandColor,
        )

    override fun printDryRunInfo(logger: Logger) {
    }

    interface UploadParams :
        PublishWorkParameters,
        GiteaCompatibleOptions

    abstract class UploadWorkAction : PublishWorkAction<UploadParams> {
        override fun publish(): PublishResult {
            with(parameters) {
                val api = GiteaCompatibleApi(accessToken.get(), apiEndpoint.orNull ?: "https://codeberg.org/api/v1", repository.get())
                val (release, created) = getOrCreateRelease(api)

                val files = additionalFiles.files.toMutableList()

                if (file.isPresent) {
                    files.add(file.get().asFile)
                }

                val noneUnique = files.groupingBy { it.name }.eachCount().filter { it.value > 1 }
                if (noneUnique.isNotEmpty()) {
                    val noneUniqueNames = noneUnique.keys.joinToString(", ")
                    throw IllegalStateException(
                        "${hostType.get().friendlyString} file names must be unique within a release, found duplicates: $noneUniqueNames",
                    )
                }

                for (file in files) {
                    api.uploadAsset(release, file)
                }

                if (created) {
                    // Publish the release after all assets are uploaded.
                    api.publishRelease(release)
                }

                return GiteaCompatiblePublishResult(
                    repository = repository.get(),
                    releaseId = release.id,
                    url = release.htmlUrl,
                    title = announcementTitle.getOrElse("Download from ${GiteaCompatiblePlatform.CODEBERG.friendlyString}"),
                    brandColor = GiteaCompatiblePlatform.CODEBERG.defaultBrandColor,
                )
            }
        }

        data class ReleaseResult(
            val release: GiteaCompatibleApi.Release,
            val created: Boolean,
        )

        private fun getOrCreateRelease(api: GiteaCompatibleApi): ReleaseResult {
            with(parameters) {
                if (releaseResult.isPresent) {
                    val result = PublishResult.fromJson(releaseResult.get().asFile.readText()) as GiteaCompatiblePublishResult
                    return ReleaseResult(api.getRelease(result.releaseId), false)
                }

                val metadata =
                    GiteaCompatibleApi.CreateRelease(
                        body = changelog.orNull,
                        draft = true,
                        name = displayName.get(),
                        prerelease = type.get() != ReleaseType.STABLE,
                        tagName = tagName.get(),
                        targetCommitish = commitish.get(),
                    )

                return ReleaseResult(api.createRelease(metadata), true)
            }
        }
    }
}
