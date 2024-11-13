package me.modmuss50.mpp

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles

// Contains options shared by each platform and the extension
interface PublishOptions {
    @get:InputFile
    val file: RegularFileProperty

    @get:Input
    val version: Property<String> // Should this use a TextResource?

    @get:Input
    val changelog: Property<String>

    @get:Input
    val type: Property<ReleaseType>

    @get:Input
    val displayName: Property<String>

    @get:Input
    val modLoaders: ListProperty<String>

    @get:InputFiles
    val additionalFiles: ConfigurableFileCollection

    @get:Input
    val maxRetries: Property<Int>

    fun from(other: PublishOptions) {
        file.convention(other.file)
        version.convention(other.version)
        changelog.convention(other.changelog)
        type.convention(other.type)
        displayName.convention(other.displayName)
        modLoaders.convention(other.modLoaders)
        additionalFiles.convention(other.additionalFiles)
        maxRetries.convention(other.maxRetries)
    }
}
