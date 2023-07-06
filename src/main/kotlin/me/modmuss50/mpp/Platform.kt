package me.modmuss50.mpp

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class Platform @Inject constructor(private val name: String): PlatformOptions, Named {
    @get:Input
    abstract val accessToken: Property<String>

    @Input
    override fun getName(): String {
        return name
    }
}