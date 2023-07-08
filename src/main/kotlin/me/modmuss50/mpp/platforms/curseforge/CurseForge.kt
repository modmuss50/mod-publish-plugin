package me.modmuss50.mpp.platforms.curseforge

import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.path
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import java.lang.RuntimeException
import javax.inject.Inject

interface CurseForgeOptions : PlatformOptions {
    @get:Input
    val projectId: Property<String>

    @get:Input
    val minecraftVersions: ListProperty<String>

    @get:Input
    val apiEndpoint: Property<String>

    fun from(other: CurseForgeOptions) {
        super.from(other)
        projectId.set(other.projectId)
        minecraftVersions.set(other.minecraftVersions)
        apiEndpoint.set(other.apiEndpoint)
    }
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

                // TODO add modloader to game versions

                val metadata = CurseForgeApi.UploadFileMetadata(
                    changelog = changelog.get(),
                    changelogType = "markdown",
                    displayName = displayName.orNull,
                    gameVersions = gameVersions,
                    releaseType = CurseForgeApi.ReleaseType.valueOf(type.get()),
                    relations = CurseForgeApi.UploadFileRelations(projects = emptyList()), // TODO relations
                )

                val response = api.uploadFile(projectId.get(), file.path, metadata)

                // TODO additional files.
            }
        }
    }
}
