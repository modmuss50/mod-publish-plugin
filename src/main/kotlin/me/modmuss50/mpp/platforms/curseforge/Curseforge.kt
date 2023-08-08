package me.modmuss50.mpp.platforms.curseforge

import me.modmuss50.mpp.HttpUtils
import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformDependency
import me.modmuss50.mpp.PlatformDependencyContainer
import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.PlatformOptionsInternal
import me.modmuss50.mpp.PublishOptions
import me.modmuss50.mpp.path
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import javax.inject.Inject
import kotlin.reflect.KClass

interface CurseforgeOptions : PlatformOptions, PlatformOptionsInternal<CurseforgeOptions>, PlatformDependencyContainer<CurseforgeDependency> {
    @get:Input
    val projectId: Property<String>

    @get:Input
    val minecraftVersions: ListProperty<String>

    @get:Input
    val apiEndpoint: Property<String>

    fun from(other: CurseforgeOptions) {
        super.from(other)
        fromDependencies(other)
        projectId.set(other.projectId)
        minecraftVersions.set(other.minecraftVersions)
        apiEndpoint.set(other.apiEndpoint)
    }

    fun from(other: Provider<CurseforgeOptions>) {
        from(other.get())
    }

    fun from(other: Provider<CurseforgeOptions>, publishOptions: Provider<PublishOptions>) {
        from(other)
        from(publishOptions.get())
    }

    override fun setInternalDefaults() {
        apiEndpoint.convention("https://minecraft.curseforge.com")
    }

    override val platformDependencyKClass: KClass<CurseforgeDependency>
        get() = CurseforgeDependency::class
}

interface CurseforgeDependency : PlatformDependency {
    @get:Input
    val slug: Property<String>
}

abstract class Curseforge @Inject constructor(name: String) : Platform(name), CurseforgeOptions {
    override fun publish(queue: WorkQueue) {
        queue.submit(UploadWorkAction::class.java) {
            it.from(this)
        }
    }

    interface UploadParams : WorkParameters, CurseforgeOptions

    abstract class UploadWorkAction : WorkAction<UploadParams> {
        override fun execute() {
            with(parameters) {
                val api = CurseforgeApi(accessToken.get(), apiEndpoint.get())

                val gameVersionTypes = HttpUtils.retry(maxRetries.get(), "Failed to get game version types") {
                    api.getVersionTypes()
                }.filter {
                    it.slug.startsWith("minecraft") || it.slug == "java" || it.slug == "modloader"
                }.map {
                    it.id
                }

                val availableGameVersions = HttpUtils.retry(maxRetries.get(), "Failed to get game versions") {
                    api.getGameVersions()
                }.filter {
                    gameVersionTypes.contains(it.gameVersionTypeID)
                }

                val gameVersions = ArrayList<Int>()

                for (version in minecraftVersions.get()) {
                    val id = availableGameVersions.find { it.name == version }?.id
                        ?: throw RuntimeException("Could not find game version: $version")
                    gameVersions.add(id)
                }

                for (modLoader in modLoaders.get()) {
                    val id = availableGameVersions.find { it.name.equals(modLoader, ignoreCase = true) }?.id
                        ?: throw RuntimeException("Could not find mod loader: $modLoader")
                    gameVersions.add(id)
                }

                val projectRelations = dependencies.get().map {
                    CurseforgeApi.ProjectFileRelation(
                        slug = it.slug.get(),
                        type = CurseforgeApi.RelationType.valueOf(it.type.get()),
                    )
                }

                val relations = if (projectRelations.isNotEmpty()) {
                    CurseforgeApi.UploadFileRelations(
                        projects = projectRelations,
                    )
                } else {
                    null
                }

                val metadata = CurseforgeApi.UploadFileMetadata(
                    changelog = changelog.get(),
                    changelogType = "markdown",
                    displayName = displayName.get(),
                    gameVersions = gameVersions,
                    releaseType = CurseforgeApi.ReleaseType.valueOf(type.get()),
                    relations = relations,
                )

                val response = HttpUtils.retry(maxRetries.get(), "Failed to upload file") {
                    api.uploadFile(projectId.get(), file.path, metadata)
                }

                for (additionalFile in additionalFiles.files) {
                    val additionalMetadata = metadata.copy(parentFileID = response.id, gameVersions = null)

                    val additionalResponse = HttpUtils.retry(maxRetries.get(), "Failed to upload additional file") {
                        api.uploadFile(projectId.get(), additionalFile.toPath(), additionalMetadata)
                    }
                }
            }
        }
    }
}
