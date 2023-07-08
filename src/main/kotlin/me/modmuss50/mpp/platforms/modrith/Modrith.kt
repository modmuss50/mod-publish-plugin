package me.modmuss50.mpp.platforms.modrith

import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.path
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import java.nio.file.Path
import javax.inject.Inject

interface ModrithOptions : PlatformOptions {
    @get:Input
    val projectId: Property<String>

    @get:Input
    val minecraftVersions: ListProperty<String>

    @get:Input
    val featured: Property<Boolean>

    @get:Input
    val apiEndpoint: Property<String>

    fun from(other: ModrithOptions) {
        super.from(other)
        projectId.set(other.projectId)
        minecraftVersions.set(other.minecraftVersions)
        featured.set(other.featured)
        apiEndpoint.set(other.apiEndpoint)
    }
}

abstract class Modrith @Inject constructor(name: String) : Platform(name), ModrithOptions {
    init {
        featured.convention(false)
        apiEndpoint.convention("https://api.modrinth.com")
    }

    override fun publish(queue: WorkQueue) {
        queue.submit(UploadWorkAction::class.java) {
            it.from(this)
        }
    }

    interface UploadParams : WorkParameters, ModrithOptions

    abstract class UploadWorkAction : WorkAction<UploadParams> {
        override fun execute() {
            with(parameters) {
                val api = ModrithApi(accessToken.get(), apiEndpoint.get())

                val primaryFileKey = "primaryFile"
                val files = HashMap<String, Path>()
                files[primaryFileKey] = file.path

                additionalFiles.files.forEachIndexed { index, additionalFile ->
                    files["file_$index"] = additionalFile.toPath()
                }

                val metadata = ModrithApi.CreateVersion(
                    name = displayName.getOrElse(file.get().asFile.name),
                    versionNumber = version.get(),
                    changelog = changelog.orNull,
                    dependencies = emptyList(), // TODO
                    gameVersions = minecraftVersions.get(),
                    versionType = ModrithApi.VersionType.valueOf(type.get()),
                    loaders = emptyList(), // TODO
                    featured = featured.get(),
                    projectId = projectId.get(),
                    fileParts = files.keys.toList(),
                    primaryFile = primaryFileKey,
                )

                // TODO get response
                api.createVersion(metadata, files)
            }
        }
    }
}
