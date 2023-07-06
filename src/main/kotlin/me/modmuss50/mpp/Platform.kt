package me.modmuss50.mpp

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.workers.WorkQueue
import javax.inject.Inject

interface PlatformOptions: PublishOptions {
    @get:Input
    val accessToken: Property<String>

    fun from(other: PlatformOptions) {
        super.from(other)
        accessToken.set(other.accessToken)
    }
}

abstract class Platform @Inject constructor(private val name: String): Named, PlatformOptions {
    abstract fun publish(queue: WorkQueue)

    @Input
    override fun getName(): String {
        return name
    }
}