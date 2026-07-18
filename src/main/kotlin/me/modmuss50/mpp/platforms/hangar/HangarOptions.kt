package me.modmuss50.mpp.platforms.hangar

import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.platforms.hangar.dependency.HangarDependencyContainer
import me.modmuss50.mpp.platforms.hangar.file.HangarFileContainer
import org.gradle.api.Incubating
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider

/*
* https://github.com/HangarMC/hangar-publish-plugin/blob/master/plugin/src/main/kotlin/io/papermc/hangarpublishplugin/model/HangarPublication.kt
*/
@Incubating
interface HangarOptions : HangarFileContainer, HangarDependencyContainer {
    @get:InputFile
    @get:Optional
    val file: RegularFileProperty

    @get:Input
    @get:Optional
    val apiEndpoint: Property<String>

    @get:Input
    @get:Optional
    val allowEmptyFiles: Property<Boolean>

    @get:Input
    val accessToken: Property<String>

    @get:Input
    val channel: Property<String>

    @get:Input
    @get:Optional
    val description: Property<String>

    @get:Input
    @get:Optional
    val platformDependencies: MapProperty<String, List<String>>

    @get:Input
    val version: Property<String>

    fun setInternalDefaults() {
        apiEndpoint.convention("https://hangar.papermc.io/api/v1/")
        allowEmptyFiles.convention(true)
    }

    fun from(other: HangarOptions) {
        fromFiles(other)
        fromDependencies(other)
        file.convention(other.file)
        apiEndpoint.convention(other.apiEndpoint)
        allowEmptyFiles.convention(other.allowEmptyFiles)
        accessToken.convention(other.accessToken)
        channel.convention(other.channel)
        description.convention(other.description)
        platformDependencies.convention(other.platformDependencies)
        version.convention(other.version)
    }

    fun from(other: Provider<HangarOptions>) {
        from(other.get())
    }

    /**
     * Publish to an existing release, created by another task.
     */
    fun parent(task: TaskProvider<Task>) {
        val publishTask = task.map { it as PublishModTask }
        val options = publishTask.map { it.platform as HangarOptions }

        channel.set(options.flatMap { it.channel })
        channel.finalizeValue()
        description.set(options.flatMap { it.description })
        description.finalizeValue()
        version.set(options.flatMap { it.version })
        version.finalizeValue()
    }
}
