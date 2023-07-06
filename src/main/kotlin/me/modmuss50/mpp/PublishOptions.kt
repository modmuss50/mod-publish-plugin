package me.modmuss50.mpp

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
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

    @get:InputFiles
    val additionalFiles: ConfigurableFileCollection

    fun from(other: PublishOptions) {
        file.set(other.file)
        version.set(other.version)
        changelog.set(other.changelog)
        type.set(other.type)
        additionalFiles.setFrom(other.additionalFiles)
    }

    enum class ReleaseType {
        STABLE,
        BETA,
        ALPHA,
    }
}
