package me.modmuss50.mpp.platforms.curseforge

import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformDependency
import me.modmuss50.mpp.PlatformDependencyContainer
import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.path
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import javax.inject.Inject
import kotlin.reflect.KClass

interface CurseForgeOptions : PlatformOptions, PlatformDependencyContainer<CurseForgeDependency> {
    @get:Input
    val projectId: Property<String>

    @get:Input
    val minecraftVersions: ListProperty<String>

    @get:Input
    val apiEndpoint: Property<String>

    fun from(other: CurseForgeOptions) {
        super.from(other)
        fromDependencies(other)
        projectId.set(other.projectId)
        minecraftVersions.set(other.minecraftVersions)
        apiEndpoint.set(other.apiEndpoint)
    }

    override val platformDependencyKClass: KClass<CurseForgeDependency>
        get() = CurseForgeDependency::class
}

interface CurseForgeDependency : PlatformDependency {
    @get:Input
    val slug: Property<String>
}

abstract class CurseForge @Inject constructor(name: String) : Platform(name), CurseForgeOptions {
    init {
        apiEndpoint.convention("https://minecraft.curseforge.com")
    }

    override fun publish(queue: WorkQueue) {
        queue.submit(UploadWorkAction::class.java) {
            it.from(this)
        }
    }

    interface UploadParams : WorkParameters, CurseForgeOptions

    abstract class UploadWorkAction : WorkAction<UploadParams> {
        override fun execute() {
            with(parameters) {
                val api = CurseForgeApi(accessToken.get(), apiEndpoint.get())
                val availableGameVersions = api.getGameVersions()

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
                    CurseForgeApi.ProjectFileRelation(
                        slug = it.slug.get(),
                        type = CurseForgeApi.RelationType.valueOf(it.type.get()),
                    )
                }

                val metadata = CurseForgeApi.UploadFileMetadata(
                    changelog = changelog.get(),
                    changelogType = "markdown",
                    displayName = displayName.orNull,
                    gameVersions = gameVersions,
                    releaseType = CurseForgeApi.ReleaseType.valueOf(type.get()),
                    relations = CurseForgeApi.UploadFileRelations(projects = projectRelations),
                )

                val response = api.uploadFile(projectId.get(), file.path, metadata)

                // TODO additional files.
            }
        }
    }
}
