package me.modmuss50.mpp

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class ModPublishExtension(val project: Project): PlatformOptions {
    abstract val allowNoneSemver: Property<Boolean>
    abstract val dryRun: Property<Boolean>
    abstract val maxRetries: Property<Int>
    val platforms: ExtensiblePolymorphicDomainObjectContainer<Platform> = project.objects.polymorphicDomainObjectContainer(Platform::class.java)

    init {
        // Inherit the platform options from this extension.
        // TODO we may need to do this in the platform factory
        platforms.whenObjectAdded {
            it.from(this)
        }
    }
}